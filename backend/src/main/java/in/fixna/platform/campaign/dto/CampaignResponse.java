package in.fixna.platform.campaign.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignObjective;
import in.fixna.platform.campaign.CampaignStatus;

/** Campaign response DTO. Tenant id stays server-side, never serialized. */
public record CampaignResponse(
        UUID id,
        UUID businessId,
        String name,
        CampaignObjective objective,
        CampaignStatus status,
        BigDecimal totalBudget,
        String currency,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String externalReference) {

    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getBusinessId(),
                campaign.getName(),
                campaign.getObjective(),
                campaign.getStatus(),
                campaign.getTotalBudget(),
                campaign.getCurrency(),
                campaign.getStartAt(),
                campaign.getEndAt(),
                campaign.getExternalReference());
    }
}
