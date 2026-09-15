package in.fixna.platform.ai;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import in.fixna.platform.billing.PlanLimitChecker;
import in.fixna.platform.billing.PlanLimits;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * WF09 AI quota gate: counts usage rows in the trailing 24h window against
 * the tenant's plan allowance and fails closed with AI_QUOTA_EXCEEDED.
 */
@ExtendWith(MockitoExtension.class)
class AiQuotaCheckerTest {

    @Mock AiUsageRepository usage;
    @Mock PlanLimitChecker plans;

    private AiQuotaChecker checker;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        checker = new AiQuotaChecker(usage, plans);
        TenantContext.set(tenantId, userId, MembershipRole.TENANT_OWNER);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void quotaExhaustedFailsClosed() {
        when(plans.limitsForCurrentTenant()).thenReturn(PlanLimits.free()); // 10/day
        when(usage.countByTenantIdAndCreatedAtAfter(any(UUID.class), any(OffsetDateTime.class)))
                .thenReturn(10L);

        assertThatThrownBy(() -> checker.checkCurrentTenant())
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_QUOTA_EXCEEDED");
    }

    @Test
    void headroomAllowsCall() {
        when(plans.limitsForCurrentTenant()).thenReturn(PlanLimits.free());
        when(usage.countByTenantIdAndCreatedAtAfter(any(UUID.class), any(OffsetDateTime.class)))
                .thenReturn(3L);

        assertThatCode(() -> checker.checkCurrentTenant()).doesNotThrowAnyException();
    }
}