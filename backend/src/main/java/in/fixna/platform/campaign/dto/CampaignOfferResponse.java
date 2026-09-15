package in.fixna.platform.campaign.dto;

import java.util.UUID;

import in.fixna.platform.campaign.CampaignOffer;

/** Offer response DTO. */
public record CampaignOfferResponse(UUID id, String title, String description, String promoCode) {

    public static CampaignOfferResponse from(CampaignOffer offer) {
        return new CampaignOfferResponse(
                offer.getId(), offer.getTitle(), offer.getDescription(), offer.getPromoCode());
    }
}
