package in.fixna.platform.tenant;

/**
 * Membership roles. Write access covers business/campaign management;
 * VIEWER is read-only. PLATFORM_ADMIN is resolved separately and never
 * stored as a tenant membership role.
 */
public enum MembershipRole {
    TENANT_OWNER,
    TENANT_ADMIN,
    TENANT_MARKETING_MANAGER,
    TENANT_MARKETING_USER,
    TENANT_VIEWER,
    AGENCY_ADMIN,
    AGENCY_USER;

    /** Whether the role may mutate tenant-owned resources. */
    public boolean canWrite() {
        return this != TENANT_VIEWER;
    }

    /** Whether the role may manage memberships and tenant settings. */
    public boolean canAdminister() {
        return this == TENANT_OWNER || this == TENANT_ADMIN || this == AGENCY_ADMIN;
    }
}
