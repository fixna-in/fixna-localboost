package in.fixna.platform.audience.dto;

import java.util.Map;
import java.util.UUID;

import in.fixna.platform.audience.Audience;

/** Audience response DTO. Tenant id stays server-side. */
public record AudienceResponse(UUID id, UUID campaignId, String name, Map<String, Object> definition) {

    public static AudienceResponse from(Audience audience) {
        return new AudienceResponse(
                audience.getId(),
                audience.getCampaignId(),
                audience.getName(),
                audience.getDefinition());
    }
}