package in.fixna.platform.common.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.TenantRepository;
import in.fixna.platform.tenant.TenantType;

/**
 * Tenant-scoped audit-log reads (WF09). Regular tenants read only their own
 * rows from {@link TenantContext}; platform admins (INTERNAL tenant) may pass
 * an explicit tenant filter after the platform gate.
 */
@Service
public class AuditViewerService {

    private final AuditLogRepository logs;
    private final TenantRepository tenants;

    public AuditViewerService(AuditLogRepository logs, TenantRepository tenants) {
        this.logs = logs;
        this.tenants = tenants;
    }

    /** Tenant-scoped page of audit events, newest first, optional action filter. */
    @Transactional(readOnly = true)
    public Page<AuditLog> listForCurrentTenant(String action, int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();
        return page(tenantId, action, page, size);
    }

    /** Platform-admin page over any tenant's audit events (INTERNAL gate first). */
    @Transactional(readOnly = true)
    public Page<AuditLog> listForTenant(UUID tenantId, String action, int page, int size) {
        requirePlatformAdmin();
        if (tenantId == null) {
            throw new FixnaException("TENANT_REQUIRED", HttpStatus.BAD_REQUEST, "tenantId is required");
        }
        return page(tenantId, action, page, size);
    }

    /** Fail-closed gate: caller must belong to an INTERNAL tenant. */
    void requirePlatformAdmin() {
        UUID callerTenant = TenantContext.requireTenantId();
        var tenant = tenants.findById(callerTenant)
                .orElseThrow(() -> new FixnaException(
                        "FORBIDDEN", HttpStatus.FORBIDDEN, "Platform admin access required"));
        if (tenant.getTenantType() != TenantType.INTERNAL) {
            throw new FixnaException(
                    "FORBIDDEN", HttpStatus.FORBIDDEN, "Platform admin access required");
        }
    }

    private Page<AuditLog> page(UUID tenantId, String action, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (action == null || action.isBlank()) {
            return logs.findByTenantId(tenantId, pageable);
        }
        return logs.findByTenantIdAndAction(tenantId, action.trim(), pageable);
    }

    /** Maps audit metadata details for persistence (string-only, never secrets). */
    static String metadataJson(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : details.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append('"').append(escape(entry.getKey())).append("\":\"")
                    .append(escape(entry.getValue())).append('"');
        }
        return out.append('}').toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Persists one audit event row; failures are swallowed by the publisher, not here. */
    @Transactional
    public void record(
            UUID tenantId, UUID actorUserId, String action, String entityType,
            String entityId, Map<String, String> details, OffsetDateTime occurredAt) {
        AuditLog row = new AuditLog();
        row.setTenantId(tenantId);
        row.setUserId(actorUserId);
        row.setAction(action);
        row.setResourceType(entityType);
        if (entityId != null) {
            try {
                row.setResourceId(UUID.fromString(entityId));
            } catch (IllegalArgumentException ignored) {
                row.setResourceId(null);
            }
        }
        row.setMetadata(metadataJson(details));
        if (occurredAt != null) {
            row.setCreatedAt(occurredAt);
        }
        logs.save(row);
    }
}
