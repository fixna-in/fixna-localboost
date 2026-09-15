package in.fixna.platform.campaign;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignOfferRepository extends JpaRepository<CampaignOffer, UUID> {

    List<CampaignOffer> findByTenantIdAndCampaignId(UUID tenantId, UUID campaignId);
}
