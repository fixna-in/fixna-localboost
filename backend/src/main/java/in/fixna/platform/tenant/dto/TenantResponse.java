package in.fixna.platform.tenant.dto;

import java.util.UUID;

import in.fixna.platform.tenant.Tenant;
import in.fixna.platform.tenant.TenantType;

/** Tenant response DTO. Never exposes other tenants. */
public record TenantResponse(UUID id, String name, TenantType tenantType) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getTenantType());
    }
}
