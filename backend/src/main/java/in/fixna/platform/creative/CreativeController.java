package in.fixna.platform.creative;

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
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.creative.dto.CreativeRequest;
import in.fixna.platform.creative.dto.CreativeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Creative endpoints nested under campaigns (US-011). Controllers only
 * translate HTTP — tenant scope, campaign ownership and content rules live
 * in {@link CreativeService}.
 */
@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/creatives")
@Tag(name = "creatives", description = "Tenant + campaign-scoped creative drafts (US-011)")
public class CreativeController {

    private final CreativeService creativeService;

    public CreativeController(CreativeService creativeService) {
        this.creativeService = creativeService;
    }

    @Operation(summary = "List creatives for a campaign")
    @GetMapping
    public ResponseEntity<List<CreativeResponse>> list(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(creativeService.list(campaignId));
    }

    @Operation(summary = "Create a creative draft (AI output saved after review)")
    @PostMapping
    public ResponseEntity<CreativeResponse> create(
            @PathVariable UUID campaignId, @Valid @RequestBody CreativeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creativeService.create(campaignId, request));
    }

    @Operation(summary = "Update creative content or status (DRAFT/READY)")
    @PutMapping("/{creativeId}")
    public ResponseEntity<CreativeResponse> update(
            @PathVariable UUID campaignId,
            @PathVariable UUID creativeId,
            @Valid @RequestBody CreativeRequest request) {
        return ResponseEntity.ok(creativeService.update(campaignId, creativeId, request));
    }

    @Operation(summary = "Delete a creative")
    @DeleteMapping("/{creativeId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID campaignId, @PathVariable UUID creativeId) {
        creativeService.delete(campaignId, creativeId);
        return ResponseEntity.noContent().build();
    }
}