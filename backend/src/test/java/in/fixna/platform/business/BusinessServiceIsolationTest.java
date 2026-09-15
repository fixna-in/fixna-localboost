package in.fixna.platform.business;

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

import in.fixna.platform.business.dto.BusinessRequest;
import in.fixna.platform.business.dto.BusinessResponse;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tenant-isolation contract tests for businesses: tenant A cannot see,
 * update or delete tenant B records; viewer role cannot write.
 */
@ExtendWith(MockitoExtension.class)
class BusinessServiceIsolationTest {

    @Mock BusinessRepository businesses;
    @Mock BusinessLocationRepository locations;
    @Mock AuditPublisher audit;
    @Mock in.fixna.platform.billing.PlanLimitChecker planLimits;

    @InjectMocks BusinessService service;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID userA = UUID.randomUUID();

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void listIsScopedToCallerTenant() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_USER);
        Business owned = business(tenantA);
        when(businesses.findByTenantId(tenantA)).thenReturn(List.of(owned));

        List<BusinessResponse> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(owned.getId());
    }

    @Test
    void crossTenantReadSurfacesNotFound() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_USER);
        UUID businessOfB = UUID.randomUUID();
        when(businesses.findByIdAndTenantId(businessOfB, tenantA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(businessOfB))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void crossTenantUpdateDeniedAsNotFound() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_USER);
        UUID businessOfB = UUID.randomUUID();
        when(businesses.findByIdAndTenantId(businessOfB, tenantA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                        businessOfB, new BusinessRequest("Hijack", null, null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("BUSINESS_NOT_FOUND");
    }

    @Test
    void viewerCannotCreateBusiness() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_VIEWER);

        assertThatThrownBy(() -> service.create(new BusinessRequest("Nope", null, null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Business business(UUID tenantId) {
        Business business = new Business();
        business.setTenantId(tenantId);
        business.setName("Sharma Sweets");
        return business;
    }
}
