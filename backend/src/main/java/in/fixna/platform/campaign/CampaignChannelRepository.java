package in.fixna.platform.campaign;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignChannelRepository extends JpaRepository<CampaignChannel, UUID> {

    List<CampaignChannel> findByTenantIdAndCampaignId(UUID tenantId, UUID campaignId);
}
