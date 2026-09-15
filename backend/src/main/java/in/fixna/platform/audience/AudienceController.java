package in.fixna.platform.audience;

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

import in.fixna.platform.audience.dto.AudienceRequest;
import in.fixna.platform.audience.dto.AudienceResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Audience endpoints nested under a campaign (US-008). */
@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/audiences")
@Tag(name = "audiences", description = "Tenant-scoped audience definitions for a campaign")
public class AudienceController {

    private final AudienceService audienceService;

    public AudienceController(AudienceService audienceService) {
        this.audienceService = audienceService;
    }

    @Operation(summary = "List audiences of a campaign")
    @GetMapping
    public ResponseEntity<List<AudienceResponse>> list(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(audienceService.list(campaignId));
    }

    @Operation(summary = "Create an audience for a campaign")
    @PostMapping
    public ResponseEntity<AudienceResponse> create(
            @PathVariable UUID campaignId, @Valid @RequestBody AudienceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(audienceService.create(campaignId, request));
    }

    @Operation(summary = "Update an audience")
    @PutMapping("/{audienceId}")
    public ResponseEntity<AudienceResponse> update(
            @PathVariable UUID campaignId,
            @PathVariable UUID audienceId,
            @Valid @RequestBody AudienceRequest request) {
        return ResponseEntity.ok(audienceService.update(campaignId, audienceId, request));
    }

    @Operation(summary = "Delete an audience")
    @DeleteMapping("/{audienceId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID campaignId, @PathVariable UUID audienceId) {
        audienceService.delete(campaignId, audienceId);
        return ResponseEntity.noContent().build();
    }
}