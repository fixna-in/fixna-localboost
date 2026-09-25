package in.fixna.platform.admin;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.billing.PlanCode;
import in.fixna.platform.billing.Subscription;
import in.fixna.platform.billing.SubscriptionService;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.MembershipRole;
import in.fixna.platform.tenant.Tenant;
import in.fixna.platform.tenant.TenantMembershipRepository;
import in.fixna.platform.tenant.TenantRepository;
import in.fixna.platform.tenant.TenantType;
import in.fixna.platform.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock TenantRepository tenants;
    @Mock TenantMembershipRepository memberships;
    @Mock UserRepository users;
    @Mock SubscriptionService subscriptions;

    @InjectMocks AdminService adminService;

    private final UUID internalTenantId = UUID.randomUUID();
    private final UUID targetTenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void parsePlanCodeRejectsUnknownValues() {
        assertThatThrownBy(() -> AdminService.parsePlanCode("enterprise"))
                .isInstanceOf(FixnaException.class)
                .satisfies(ex -> {
                    FixnaException failure = (FixnaException) ex;
                    assertThat(failure.getCode()).isEqualTo("INVALID_PLAN_CODE");
                    assertThat(failure.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void updatePlanRequiresPlatformAdminAndValidPlanCode() {
        TenantContext.set(internalTenantId, userId, MembershipRole.TENANT_OWNER);
        Tenant internal = new Tenant();
        internal.setId(internalTenantId);
        internal.setTenantType(TenantType.INTERNAL);
        when(tenants.findById(internalTenantId)).thenReturn(Optional.of(internal));
        Subscription updated = new Subscription();
        updated.setTenantId(targetTenantId);
        updated.setPlanCode(PlanCode.GROWTH.name());
        updated.setStatus("ACTIVE");
        when(subscriptions.updatePlan(eq(targetTenantId), eq(PlanCode.GROWTH))).thenReturn(updated);

        Subscription result = adminService.updatePlan(targetTenantId, "growth");

        assertThat(result.getPlanCode()).isEqualTo("GROWTH");
        verify(subscriptions).updatePlan(targetTenantId, PlanCode.GROWTH);
    }

    @Test
    void nonInternalTenantCannotUpdatePlans() {
        UUID smbTenant = UUID.randomUUID();
        TenantContext.set(smbTenant, userId, MembershipRole.TENANT_OWNER);
        Tenant smb = new Tenant();
        smb.setId(smbTenant);
        smb.setTenantType(TenantType.SMB);
        when(tenants.findById(smbTenant)).thenReturn(Optional.of(smb));

        assertThatThrownBy(() -> adminService.updatePlan(targetTenantId, "STARTER"))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
