package in.fixna.platform.lead.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.lead.Lead;
import in.fixna.platform.lead.LeadStatus;

/** Lead response. Tenant id stays server-side, never serialized. */
public record LeadResponse(
        UUID id,
        UUID businessId,
        UUID campaignId,
        String name,
        String phone,
        String email,
        LeadStatus status,
        String source,
        OffsetDateTime createdAt) {

    public static LeadResponse from(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getBusinessId(),
                lead.getCampaignId(),
                lead.getName(),
                lead.getPhone(),
                lead.getEmail(),
                lead.getStatus(),
                lead.getSource(),
                lead.getCreatedAt());
    }
}
