package in.fixna.platform.common.tenant;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.MembershipRole;

/**
 * Request-scoped tenant/user identity resolved from the authenticated JWT
 * (server-issued claims — never from client-supplied parameters).
 *
 * <p>Populated by the security filter chain; every tenant-owned repository
 * call must resolve scope from here. Always cleared at the end of the
 * request by the authentication filter.
 */
public final class TenantContext {

    private record Scope(UUID tenantId, UUID userId, MembershipRole role) {}

    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId, UUID userId) {
        set(tenantId, userId, MembershipRole.TENANT_VIEWER);
    }

    public static void set(UUID tenantId, UUID userId, MembershipRole role) {
        if (tenantId == null || userId == null) {
            throw new FixnaException(
                    "TENANT_CONTEXT_INVALID",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tenant context requires both tenant and user identity");
        }
        CURRENT.set(new Scope(tenantId, userId, role == null ? MembershipRole.TENANT_VIEWER : role));
    }

    /** Returns the current tenant id or throws when no request scope exists. */
    public static UUID requireTenantId() {
        Scope scope = CURRENT.get();
        if (scope == null) {
            throw new FixnaException(
                    "TENANT_CONTEXT_MISSING",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tenant context is not available for this request");
        }
        return scope.tenantId();
    }

    /** Returns the current user id or throws when no request scope exists. */
    public static UUID requireUserId() {
        Scope scope = CURRENT.get();
        if (scope == null) {
            throw new FixnaException(
                    "TENANT_CONTEXT_MISSING",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tenant context is not available for this request");
        }
        return scope.userId();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Current role or VIEWER when no scope exists (used for fail-closed checks). */
    public static MembershipRole currentRole() {
        Scope scope = CURRENT.get();
        return scope == null ? MembershipRole.TENANT_VIEWER : scope.role();
    }

    /** Throws FORBIDDEN unless the current role may mutate tenant resources. */
    public static void requireWrite() {
        if (!currentRole().canWrite()) {
            throw new FixnaException("FORBIDDEN", HttpStatus.FORBIDDEN, "Write access denied for this role");
        }
    }

    /** Throws FORBIDDEN unless the current role may manage memberships/settings. */
    public static void requireAdmin() {
        if (!currentRole().canAdminister()) {
            throw new FixnaException("FORBIDDEN", HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
