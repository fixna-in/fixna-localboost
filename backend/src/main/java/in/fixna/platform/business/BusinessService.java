package in.fixna.platform.business;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.billing.PlanLimitChecker;
import in.fixna.platform.business.dto.BusinessLocationRequest;
import in.fixna.platform.business.dto.BusinessLocationResponse;
import in.fixna.platform.business.dto.BusinessRequest;
import in.fixna.platform.business.dto.BusinessResponse;
import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;

/**
 * US-004/US-005: tenant-scoped business + location management. Every lookup
 * is (tenantId, id) — never id alone — and location writes re-verify the
 * parent business belongs to the caller's tenant.
 */
@Service
public class BusinessService {

    private final BusinessRepository businesses;
    private final BusinessLocationRepository locations;
    private final AuditPublisher audit;
    private final PlanLimitChecker planLimits;

    public BusinessService(
            BusinessRepository businesses,
            BusinessLocationRepository locations,
            AuditPublisher audit,
            PlanLimitChecker planLimits) {
        this.businesses = businesses;
        this.locations = locations;
        this.audit = audit;
        this.planLimits = planLimits;
    }

    @Transactional(readOnly = true)
    public List<BusinessResponse> list() {
        UUID tenantId = TenantContext.requireTenantId();
        return businesses.findByTenantId(tenantId).stream().map(BusinessResponse::from).toList();
    }

    @Transactional
    public BusinessResponse create(BusinessRequest request) {
        TenantContext.requireWrite();
        UUID tenantId = TenantContext.requireTenantId();
        planLimits.checkBusinessCreate();
        Business business = new Business();
        business.setTenantId(tenantId);
        apply(business, request);
        businesses.save(business);
        audit.publish(new AuditEvent(
                "business.created", tenantId, TenantContext.requireUserId(), "business",
                business.getId().toString(), Map.of(), null));
        return BusinessResponse.from(business);
    }

    @Transactional(readOnly = true)
    public BusinessResponse get(UUID id) {
        return BusinessResponse.from(requireOwned(id));
    }

    @Transactional
    public BusinessResponse update(UUID id, BusinessRequest request) {
        TenantContext.requireWrite();
        Business business = requireOwned(id);
        apply(business, request);
        businesses.save(business);
        audit.publish(new AuditEvent(
                "business.updated", business.getTenantId(), TenantContext.requireUserId(), "business",
                business.getId().toString(), Map.of(), null));
        return BusinessResponse.from(business);
    }

    @Transactional
    public void delete(UUID id) {
        TenantContext.requireWrite();
        Business business = requireOwned(id);
        // Locations reference the business without ON DELETE CASCADE — remove
        // children first so tenant-scoped delete never hits an FK violation.
        locations.findByTenantIdAndBusinessId(business.getTenantId(), business.getId())
                .forEach(locations::delete);
        businesses.delete(business);
        audit.publish(new AuditEvent(
                "business.deleted", business.getTenantId(), TenantContext.requireUserId(), "business",
                business.getId().toString(), Map.of(), null));
    }

    @Transactional(readOnly = true)
    public List<BusinessLocationResponse> listLocations(UUID businessId) {
        UUID tenantId = TenantContext.requireTenantId();
        requireOwned(businessId);
        return locations.findByTenantIdAndBusinessId(tenantId, businessId).stream()
                .map(BusinessLocationResponse::from)
                .toList();
    }

    @Transactional
    public BusinessLocationResponse addLocation(UUID businessId, BusinessLocationRequest request) {
        TenantContext.requireWrite();
        Business business = requireOwned(businessId);
        BusinessLocation location = new BusinessLocation();
        location.setTenantId(business.getTenantId());
        location.setBusinessId(business.getId());
        apply(location, request);
        locations.save(location);
        audit.publish(new AuditEvent(
                "business.location_added", business.getTenantId(), TenantContext.requireUserId(),
                "business_location", location.getId().toString(),
                Map.of("business", business.getId().toString()), null));
        return BusinessLocationResponse.from(location);
    }

    private void apply(Business business, BusinessRequest request) {
        business.setName(request.name().trim());
        business.setCategory(request.category());
        business.setDescription(request.description());
        business.setWebsiteUrl(request.websiteUrl());
        business.setPhone(request.phone());
    }

    private void apply(BusinessLocation location, BusinessLocationRequest request) {
        location.setAddressLine(request.addressLine());
        location.setCity(request.city());
        location.setState(request.state());
        location.setPostalCode(request.postalCode());
        if (request.country() != null && !request.country().isBlank()) {
            location.setCountry(request.country());
        }
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
    }

    /** Tenant-scoped fetch — cross-tenant ids surface as NOT_FOUND (no leakage). */
    private Business requireOwned(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return businesses
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "BUSINESS_NOT_FOUND", HttpStatus.NOT_FOUND, "Business not found"));
    }
}
