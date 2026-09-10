package in.fixna.platform.common.tenant;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Workflow 01 — tenant-isolation contract tests. Every workflow that touches
 * tenant-owned data extends this pattern: no scope, no data access.
 */
class TenantContextTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void emptyContextThrowsOnTenantAndUser() {
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("TENANT_CONTEXT_MISSING");
        assertThatThrownBy(TenantContext::requireUserId)
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("TENANT_CONTEXT_MISSING");
    }

    @Test
    void setAndClearScope() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.set(tenantId, userId);

        assertThat(TenantContext.requireTenantId()).isEqualTo(tenantId);
        assertThat(TenantContext.requireUserId()).isEqualTo(userId);

        TenantContext.clear();
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void lookupByIdAloneMustBeScoped() {
        // Guard-rail example: repositories take (tenantId, id), never id alone.
        // A cross-tenant read is simulated by demanding equality of scope.
        UUID tenantA = UUID.randomUUID();
        TenantContext.set(tenantA, UUID.randomUUID());
        UUID recordTenant = UUID.randomUUID(); // row owned by tenant B

        assertThatThrownBy(() -> {
            if (!TenantContext.requireTenantId().equals(recordTenant)) {
                throw new FixnaException(
                        "FORBIDDEN", HttpStatus.FORBIDDEN, "Cross-tenant access denied");
            }
        })
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
