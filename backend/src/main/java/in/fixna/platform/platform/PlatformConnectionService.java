package in.fixna.platform.platform;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.platform.dto.PlatformConnectionRequest;
import in.fixna.platform.platform.dto.PlatformConnectionResponse;

/**
 * Per-tenant platform connection management. Only registered platforms can
 * be connected; mock mode requires no credentials and stores none. Every
 * lookup is (tenantId, platform) — never id alone.
 */
@Service
public class PlatformConnectionService {

    private final PlatformConnectionRepository connections;
    private final PlatformAdapterRegistry registry;
    private final AuditPublisher audit;

    public PlatformConnectionService(
            PlatformConnectionRepository connections,
            PlatformAdapterRegistry registry,
            AuditPublisher audit) {
        this.connections = connections;
        this.registry = registry;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<PlatformConnectionResponse> list() {
        UUID tenantId = TenantContext.requireTenantId();
        return connections.findByTenantId(tenantId).stream()
                .map(PlatformConnectionResponse::from)
                .toList();
    }

    /** Marks the platform connected for the tenant (mock: no credentials). */
    @Transactional
    public PlatformConnectionResponse connect(PlatformConnectionRequest request) {
        TenantContext.requireWrite();
        UUID tenantId = TenantContext.requireTenantId();
        String platform = normalize(request.platform());
        if (!registry.supports(platform)) {
            throw new FixnaException(
                    "UNSUPPORTED_PLATFORM", HttpStatus.BAD_REQUEST,
                    "Platform is not supported: " + platform);
        }
        var existing = connections
                .findFirstByTenantIdAndPlatformOrderByCreatedAtDesc(tenantId, platform);
        if (existing.isPresent() && "CONNECTED".equals(existing.get().getStatus())) {
            throw new FixnaException(
                    "ALREADY_CONNECTED", HttpStatus.CONFLICT,
                    "Platform is already connected: " + platform);
        }
        PlatformConnection connection = existing.orElseGet(PlatformConnection::new);
        if (connection.getId() == null) {
            connection.setTenantId(tenantId);
            connection.setPlatform(platform);
        }
        connection.setExternalAccountId(request.externalAccountId());
        connection.setEncryptedAccessToken(null);
        connection.setEncryptedRefreshToken(null);
        connection.setStatus("CONNECTED");
        connections.save(connection);
        audit.publish(new AuditEvent(
                "platform.connected", tenantId, TenantContext.requireUserId(), "platform_connection",
                entityRef(connection), Map.of("platform", platform), null));
        return PlatformConnectionResponse.from(connection);
    }

    /** Marks the platform disconnected; token columns are cleared. */
    @Transactional
    public void disconnect(String platformRaw) {
        TenantContext.requireWrite();
        UUID tenantId = TenantContext.requireTenantId();
        String platform = normalize(platformRaw);
        PlatformConnection connection = connections
                .findFirstByTenantIdAndPlatformOrderByCreatedAtDesc(tenantId, platform)
                .orElseThrow(() -> new FixnaException(
                        "CONNECTION_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "No connection found for platform " + platform));
        connection.setStatus("DISCONNECTED");
        connection.setEncryptedAccessToken(null);
        connection.setEncryptedRefreshToken(null);
        connections.save(connection);
        audit.publish(new AuditEvent(
                "platform.disconnected", tenantId, TenantContext.requireUserId(), "platform_connection",
                entityRef(connection), Map.of("platform", platform), null));
    }

    /** Audit-safe entity reference: JPA assigns the id on persist; unit tests may not. */
    private String entityRef(PlatformConnection connection) {
        return connection.getId() == null ? "new" : connection.getId().toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
