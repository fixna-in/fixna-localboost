package in.fixna.platform.lead;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.lead.dto.LeadPageResponse;
import in.fixna.platform.lead.dto.LeadRequest;
import in.fixna.platform.lead.dto.LeadResponse;
import in.fixna.platform.lead.dto.LeadStatusRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lead endpoints. Collections are nested under the owning business
 * (tenant scope from JWT); item routes are tenant-scoped by id alone.
 */
@RestController
@Tag(name = "leads", description = "Lead capture, funnel and pagination")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @Operation(summary = "Capture a lead for a business (write roles)")
    @PostMapping("/api/v1/businesses/{businessId}/leads")
    public ResponseEntity<LeadResponse> create(
            @PathVariable UUID businessId, @Valid @RequestBody LeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.create(businessId, request));
    }

    @Operation(summary = "List leads (tenant-scoped, paginated, filterable)")
    @GetMapping("/api/v1/leads")
    public ResponseEntity<LeadPageResponse> list(
            @RequestParam(required = false) UUID businessId,
            @RequestParam(required = false) UUID campaignId,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leadService.list(businessId, campaignId, status, page, size));
    }

    @Operation(summary = "Get lead by id (tenant-scoped)")
    @GetMapping("/api/v1/leads/{id}")
    public ResponseEntity<LeadResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(leadService.get(id));
    }

    @Operation(summary = "Advance lead funnel status (write roles)")
    @PatchMapping("/api/v1/leads/{id}/status")
    public ResponseEntity<LeadResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody LeadStatusRequest request) {
        return ResponseEntity.ok(leadService.updateStatus(id, request));
    }

    @Operation(summary = "Delete lead (write roles)")
    @DeleteMapping("/api/v1/leads/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        leadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
