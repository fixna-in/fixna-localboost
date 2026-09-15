package in.fixna.platform.platform;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.platform.dto.PlatformConnectionRequest;
import in.fixna.platform.platform.dto.PlatformConnectionResponse;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tenant-isolation and credential-hygiene tests for platform connections:
 * scope comes from TenantContext (never params), viewer cannot connect,
 * tokens are never populated or surfaced, and disconnect clears secrets.
 */
@ExtendWith(MockitoExtension.class)
class PlatformConnectionServiceTest {

    @Mock PlatformConnectionRepository connections;
    @Mock PlatformAdapterRegistry registry;
    @Mock AuditPublisher audit;

    @InjectMocks PlatformConnectionService service;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID userA = UUID.randomUUID();

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void connectNormalizesPlatformAndStoresNoTokens() {
        TenantContext.set(tenantA, UUID.randomUUID(), MembershipRole.TENANT_MARKETING_USER);
        when(registry.supports("GOOGLE")).thenReturn(true);
        when(connections.findFirstByTenantIdAndPlatformOrderByCreatedAtDesc(tenantA, "GOOGLE"))
                .thenReturn(Optional.empty());

        PlatformConnectionResponse response = service.connect(
                new PlatformConnectionRequest(" google ", "acct-123"));

        assertThat(response.platform()).isEqualTo("GOOGLE");
        assertThat(response.status()).isEqualTo("CONNECTED");
        assertThat(response.externalAccountId()).isEqualTo("acct-123");
        verify(connections).save(any(PlatformConnection.class));
    }

    @Test
    void unsupportedPlatformRejectedBeforeSave() {
        TenantContext.set(tenantA, UUID.randomUUID(), MembershipRole.TENANT_MARKETING_USER);
        when(registry.supports("TIKTOK")).thenReturn(false);

        assertThatThrownBy(() -> service.connect(new PlatformConnectionRequest("tiktok", null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("UNSUPPORTED_PLATFORM");
        verify(connections, never()).save(any());
    }

    @Test
    void viewerCannotConnect() {
        TenantContext.set(tenantA, UUID.randomUUID(), MembershipRole.TENANT_VIEWER);

        assertThatThrownBy(() -> service.connect(new PlatformConnectionRequest("GOOGLE", null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void duplicateConnectedRejected() {
        TenantContext.set(tenantA, UUID.randomUUID(), MembershipRole.TENANT_ADMIN);
        when(registry.supports("META")).thenReturn(true);
        PlatformConnection existing = new PlatformConnection();
        existing.setPlatform("META");
        existing.setStatus("CONNECTED");
        when(connections.findFirstByTenantIdAndPlatformOrderByCreatedAtDesc(tenantA, "META"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.connect(new PlatformConnectionRequest("META", null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("ALREADY_CONNECTED");
    }

    @Test
    void crossTenantDisconnectSurfacesNotFound() {
        TenantContext.set(tenantA, UUID.randomUUID(), MembershipRole.TENANT_ADMIN);
        when(connections.findFirstByTenantIdAndPlatformOrderByCreatedAtDesc(tenantA, "META"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disconnect("META"))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CONNECTION_NOT_FOUND");
    }

    @Test
    void disconnectClearsTokensAndMarksDisconnected() {
        TenantContext.set(tenantA, UUID.randomUUID(), MembershipRole.TENANT_ADMIN);
        PlatformConnection connection = new PlatformConnection();
        connection.setTenantId(tenantA);
        connection.setPlatform("META");
        connection.setStatus("CONNECTED");
        connection.setEncryptedAccessToken("secret-access");
        connection.setEncryptedRefreshToken("secret-refresh");
        when(connections.findFirstByTenantIdAndPlatformOrderByCreatedAtDesc(tenantA, "META"))
                .thenReturn(Optional.of(connection));

        service.disconnect("META");

        assertThat(connection.getStatus()).isEqualTo("DISCONNECTED");
        assertThat(connection.getEncryptedAccessToken()).isNull();
        assertThat(connection.getEncryptedRefreshToken()).isNull();
        verify(connections).save(connection);
    }

    @Test
    void listIsScopedToCallerTenant() {
        TenantContext.set(tenantA, UUID.randomUUID(), MembershipRole.TENANT_VIEWER);
        PlatformConnection own = new PlatformConnection();
        own.setTenantId(tenantA);
        own.setPlatform("GOOGLE");
        own.setStatus("CONNECTED");
        when(connections.findByTenantId(tenantA)).thenReturn(List.of(own));

        List<PlatformConnectionResponse> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).platform()).isEqualTo("GOOGLE");
    }
}