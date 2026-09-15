package in.fixna.platform.common.audit;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.MembershipRole;
import in.fixna.platform.tenant.Tenant;
import in.fixna.platform.tenant.TenantRepository;
import in.fixna.platform.tenant.TenantType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WF09 audit viewer: regular tenants only see their own rows; INTERNAL
 * tenants may inspect any tenant after the platform gate; action filter and
 * metadata JSON mapping are correct.
 */
@ExtendWith(MockitoExtension.class)
class AuditViewerServiceTest {

    @Mock AuditLogRepository logs;
    @Mock TenantRepository tenants;

    private AuditViewerService service;
    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID internalTenant = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AuditViewerService(logs, tenants);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private AuditLog log(UUID tenantId) {
        AuditLog row = new AuditLog();
        row.setId(UUID.randomUUID());
        row.setTenantId(tenantId);
        row.setAction("campaign.created");
        return row;
    }

    private Tenant tenant(TenantType type) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setTenantType(type);
        return tenant;
    }

    @Test
    void currentTenantReadsOnlyOwnRows() {
        TenantContext.set(tenantA, userId, MembershipRole.TENANT_OWNER);
        AuditLog owned = log(tenantA);
        when(logs.findByTenantId(eq(tenantA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(owned)));

        Page<AuditLog> result = service.listForCurrentTenant(null, 0, 20);

        assertThat(result.getContent()).extracting(AuditLog::getTenantId).containsOnly(tenantA);
        verify(logs).findByTenantId(eq(tenantA), any(Pageable.class));
    }

    @Test
    void actionFilterPassesThrough() {
        TenantContext.set(tenantA, userId, MembershipRole.TENANT_OWNER);
        when(logs.findByTenantIdAndAction(eq(tenantA), eq("lead.created"), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.listForCurrentTenant("lead.created", 0, 20);

        verify(logs).findByTenantIdAndAction(eq(tenantA), eq("lead.created"), any(Pageable.class));
    }

    @Test
    void nonInternalTenantForbiddenFromAdminView() {
        TenantContext.set(tenantB, userId, MembershipRole.TENANT_OWNER);
        when(tenants.findById(tenantB)).thenReturn(Optional.of(tenant(TenantType.SMB)));

        assertThatThrownBy(() -> service.listForTenant(tenantA, null, 0, 20))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void internalTenantAllowedToAdminView() {
        TenantContext.set(internalTenant, userId, MembershipRole.TENANT_OWNER);
        when(tenants.findById(internalTenant)).thenReturn(Optional.of(tenant(TenantType.INTERNAL)));
        when(logs.findByTenantId(eq(tenantA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(log(tenantA))));

        Page<AuditLog> result = service.listForTenant(tenantA, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(logs).findByTenantId(eq(tenantA), any(Pageable.class));
    }

    @Test
    void metadataJsonEscapesQuotesAndBackslashes() {
        String json = AuditViewerService.metadataJson(
                Map.of("note", "a \"quoted\" \\ value", "id", "7"));
        assertThat(json)
                .contains("\"note\":\"a \\\"quoted\\\" \\\\ value\"")
                .contains("\"id\":\"7\"");
    }
}