package in.fixna.platform.campaign.dto;

import jakarta.validation.constraints.NotNull;

import in.fixna.platform.campaign.CampaignStatus;

/** Requested lifecycle transition. Validated against {@link CampaignStatus#canTransitionTo}. */
public record TransitionRequest(@NotNull CampaignStatus to) {}
