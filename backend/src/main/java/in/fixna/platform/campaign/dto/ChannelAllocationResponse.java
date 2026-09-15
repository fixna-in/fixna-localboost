package in.fixna.platform.campaign.dto;

import java.math.BigDecimal;
import java.util.UUID;

import in.fixna.platform.campaign.CampaignChannel;

/** Channel allocation response DTO. */
public record ChannelAllocationResponse(UUID id, String channel, BigDecimal allocatedBudget) {

    public static ChannelAllocationResponse from(CampaignChannel channel) {
        return new ChannelAllocationResponse(
                channel.getId(), channel.getChannel(), channel.getAllocatedBudget());
    }
}
