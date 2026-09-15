package in.fixna.platform.creative.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.creative.CreativeStatus;
import in.fixna.platform.creative.Creative;

/** Creative view model. Content text only, never secrets or payloads. */
public record CreativeResponse(
        UUID id,
        UUID campaignId,
        String channel,
        String headline,
        String body,
        String callToAction,
        CreativeStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static CreativeResponse from(Creative input) {
        return new CreativeResponse(
                input.getId(),
                input.getCampaignId(),
                input.getChannel(),
                input.getHeadline(),
                input.getBody(),
                input.getCallToAction(),
                input.getStatus(),
                input.getCreatedAt(),
                input.getUpdatedAt());
    }
}