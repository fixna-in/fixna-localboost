package in.fixna.platform.campaign.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Replace-all channel budget split. */
public record ChannelAllocationRequest(@NotNull @Valid List<ChannelAllocation> channels) {

    public ChannelAllocationRequest {
        channels = channels == null ? List.of() : List.copyOf(channels);
    }
}
