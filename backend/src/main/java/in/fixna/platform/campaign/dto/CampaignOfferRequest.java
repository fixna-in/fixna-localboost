package in.fixna.platform.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create-offer payload. */
public record CampaignOfferRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @Size(max = 100) String promoCode) {}
