package in.fixna.platform.campaign;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.campaign.dto.CampaignOfferRequest;
import in.fixna.platform.campaign.dto.CampaignOfferResponse;
import in.fixna.platform.campaign.dto.CampaignRequest;
import in.fixna.platform.campaign.dto.CampaignResponse;
import in.fixna.platform.campaign.dto.ChannelAllocationRequest;
import in.fixna.platform.campaign.dto.ChannelAllocationResponse;
import in.fixna.platform.campaign.dto.TransitionRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Campaign endpoints. Controllers only translate HTTP — tenant scope,
 * lifecycle guards and budget rules live in {@link CampaignService}.
 */
@RestController
@RequestMapping("/api/v1/campaigns")
@Tag(name = "campaigns", description = "Tenant-scoped campaigns, offers, channels and lifecycle")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @Operation(summary = "List campaigns (optionally filtered by business)")
    @GetMapping
    public ResponseEntity<List<CampaignResponse>> list(@RequestParam(required = false) UUID businessId) {
        return ResponseEntity.ok(campaignService.list(businessId));
    }

    @Operation(summary = "Create campaign in DRAFT (US-006)")
    @PostMapping
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.create(request));
    }

    @Operation(summary = "Get campaign by id (tenant-scoped)")
    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.get(id));
    }

    @Operation(summary = "Update campaign (DRAFT only)")
    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> update(
            @PathVariable UUID id, @Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(campaignService.update(id, request));
    }

    @Operation(summary = "Delete campaign (never approved/active)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        campaignService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lifecycle transition (approval path, US-011)")
    @PostMapping("/{id}/transitions")
    public ResponseEntity<CampaignResponse> transition(
            @PathVariable UUID id, @Valid @RequestBody TransitionRequest request) {
        return ResponseEntity.ok(campaignService.transition(id, request));
    }

    @Operation(summary = "Idempotent launch request (APPROVED/FAILED entry)")
    @PostMapping("/{id}/launch")
    public ResponseEntity<CampaignResponse> launch(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.launch(id));
    }

    @Operation(summary = "Add offer to campaign (US-007)")
    @PostMapping("/{id}/offers")
    public ResponseEntity<CampaignOfferResponse> addOffer(
            @PathVariable UUID id, @Valid @RequestBody CampaignOfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.addOffer(id, request));
    }

    @Operation(summary = "List campaign offers")
    @GetMapping("/{id}/offers")
    public ResponseEntity<List<CampaignOfferResponse>> listOffers(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.listOffers(id));
    }

    @Operation(summary = "Replace channel budget split (US-010)")
    @PutMapping("/{id}/channels")
    public ResponseEntity<List<ChannelAllocationResponse>> replaceChannels(
            @PathVariable UUID id, @Valid @RequestBody ChannelAllocationRequest request) {
        return ResponseEntity.ok(campaignService.replaceChannels(id, request));
    }

    @Operation(summary = "List channel allocations")
    @GetMapping("/{id}/channels")
    public ResponseEntity<List<ChannelAllocationResponse>> listChannels(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.listChannels(id));
    }
}
