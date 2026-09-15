package in.fixna.platform.business.dto;

import java.util.UUID;

import in.fixna.platform.business.Business;

/** Business response DTO. Tenant id stays server-side, never serialized. */
public record BusinessResponse(
        UUID id, String name, String category, String description, String websiteUrl, String phone) {

    public static BusinessResponse from(Business business) {
        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getCategory(),
                business.getDescription(),
                business.getWebsiteUrl(),
                business.getPhone());
    }
}
