package in.fixna.platform.geo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.geo.dto.GeoTargetRequest;
import in.fixna.platform.geo.dto.GeoTargetResponse;

/**
 * US-007/US-008: tenant-scoped geo targeting. Every target is bound to an
 * owned campaign; the parent campaign is verified tenant-scoped before any
 * read/write. Exactly one targeting dimension is enforced per type.
 */
@Service
public class GeoTargetService {

    private final GeoTargetRepository targets;
    private final CampaignRepository campaigns;
    private final AuditPublisher audit;

    public GeoTargetService(
            GeoTargetRepository targets, CampaignRepository campaigns, AuditPublisher audit) {
        this.targets = targets;
        this.campaigns = campaigns;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<GeoTargetResponse> list(UUID campaignId) {
        UUID tenantId = TenantContext.requireTenantId();
        Campaign campaign = requireOwnedCampaign(campaignId);
        return targets.findByTenantIdAndCampaignId(tenantId, campaign.getId()).stream()
                .map(GeoTargetResponse::from)
                .toList();
    }

    @Transactional
    public GeoTargetResponse add(UUID campaignId, GeoTargetRequest request) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwnedCampaign(campaignId);
        validate(request);
        GeoTarget target = new GeoTarget();
        target.setTenantId(campaign.getTenantId());
        target.setCampaignId(campaign.getId());
        apply(target, request);
        targets.save(target);
        audit.publish(new AuditEvent(
                "geo_target.added", campaign.getTenantId(), TenantContext.requireUserId(),
                "geo_target", target.getId().toString(),
                Map.of("campaign", campaign.getId().toString(), "type", target.getTargetType().name()),
                null));
        return GeoTargetResponse.from(target);
    }

    @Transactional
    public GeoTargetResponse update(UUID campaignId, UUID targetId, GeoTargetRequest request) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwnedCampaign(campaignId);
        validate(request);
        GeoTarget target = requireOwned(targetId);
        if (!campaign.getId().equals(target.getCampaignId())) {
            throw new FixnaException(
                    "GEO_TARGET_MISMATCH", HttpStatus.BAD_REQUEST,
                    "Geo target does not belong to this campaign");
        }
        apply(target, request);
        targets.save(target);
        audit.publish(new AuditEvent(
                "geo_target.updated", campaign.getTenantId(), TenantContext.requireUserId(),
                "geo_target", target.getId().toString(), Map.of(), null));
        return GeoTargetResponse.from(target);
    }

    @Transactional
    public void remove(UUID campaignId, UUID targetId) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwnedCampaign(campaignId);
        GeoTarget target = requireOwned(targetId);
        if (!campaign.getId().equals(target.getCampaignId())) {
            throw new FixnaException(
                    "GEO_TARGET_MISMATCH", HttpStatus.BAD_REQUEST,
                    "Geo target does not belong to this campaign");
        }
        targets.delete(target);
        audit.publish(new AuditEvent(
                "geo_target.removed", campaign.getTenantId(), TenantContext.requireUserId(),
                "geo_target", target.getId().toString(), Map.of(), null));
    }

    /** Exactly one targeting dimension must be populated for the declared type. */
    private void validate(GeoTargetRequest request) {
        boolean radius = request.latitude() != null && request.longitude() != null
                && request.radiusKm() != null;
        boolean city = isNotBlank(request.city());
        boolean postal = isNotBlank(request.postalCode());
        boolean region = isNotBlank(request.regionCode());
        boolean country = isNotBlank(request.countryCode());
        int count = (radius ? 1 : 0) + (city ? 1 : 0) + (postal ? 1 : 0)
                + (region ? 1 : 0) + (country ? 1 : 0);

        switch (request.targetType()) {
            case RADIUS -> {
                if (count != 1 || !radius) {
                    throw invalid("RADIUS requires latitude, longitude and radiusKm");
                }
                if (request.radiusKm().signum() <= 0) {
                    throw invalid("radiusKm must be positive");
                }
            }
            case CITY -> {
                if (count != 1 || !city) {
                    throw invalid("CITY requires a city");
                }
            }
            case POSTAL -> {
                if (count != 1 || !postal) {
                    throw invalid("POSTAL requires a postalCode");
                }
            }
            case REGION -> {
                if (count != 1 || !region) {
                    throw invalid("REGION requires a regionCode");
                }
            }
            case COUNTRY -> {
                if (count != 1 || !country) {
                    throw invalid("COUNTRY requires a countryCode");
                }
            }
        }
    }

    private FixnaException invalid(String detail) {
        return new FixnaException(
                "INVALID_GEO_TARGET", HttpStatus.BAD_REQUEST, "Invalid geo target: " + detail);
    }

    private void apply(GeoTarget target, GeoTargetRequest request) {
        target.setTargetType(request.targetType());
        target.setName(request.name());
        target.setLatitude(request.latitude());
        target.setLongitude(request.longitude());
        target.setRadiusKm(request.radiusKm());
        target.setCountryCode(request.countryCode());
        target.setRegionCode(request.regionCode());
        target.setCity(request.city());
        target.setPostalCode(request.postalCode());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** Tenant-scoped campaign lookup — cross-tenant ids surface as NOT_FOUND. */
    private Campaign requireOwnedCampaign(UUID campaignId) {
        UUID tenantId = TenantContext.requireTenantId();
        return campaigns
                .findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    /** Tenant-scoped target lookup. */
    private GeoTarget requireOwned(UUID targetId) {
        UUID tenantId = TenantContext.requireTenantId();
        return targets
                .findByIdAndTenantId(targetId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "GEO_TARGET_NOT_FOUND", HttpStatus.NOT_FOUND, "Geo target not found"));
    }
}