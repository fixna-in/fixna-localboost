package in.fixna.platform.geo;

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

import in.fixna.platform.geo.dto.GeoTargetRequest;
import in.fixna.platform.geo.dto.GeoTargetResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Geo targeting endpoints nested under a campaign (US-007/US-008). */
@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/geo-targets")
@Tag(name = "geo-targets", description = "Tenant-scoped geo targeting for a campaign")
public class GeoTargetController {

    private final GeoTargetService geoTargetService;

    public GeoTargetController(GeoTargetService geoTargetService) {
        this.geoTargetService = geoTargetService;
    }

    @Operation(summary = "List geo targets of a campaign")
    @GetMapping
    public ResponseEntity<List<GeoTargetResponse>> list(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(geoTargetService.list(campaignId));
    }

    @Operation(summary = "Add a geo target to a campaign")
    @PostMapping
    public ResponseEntity<GeoTargetResponse> add(
            @PathVariable UUID campaignId, @Valid @RequestBody GeoTargetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(geoTargetService.add(campaignId, request));
    }

    @Operation(summary = "Update a geo target")
    @PutMapping("/{targetId}")
    public ResponseEntity<GeoTargetResponse> update(
            @PathVariable UUID campaignId,
            @PathVariable UUID targetId,
            @Valid @RequestBody GeoTargetRequest request) {
        return ResponseEntity.ok(geoTargetService.update(campaignId, targetId, request));
    }

    @Operation(summary = "Remove a geo target")
    @DeleteMapping("/{targetId}")
    public ResponseEntity<Void> remove(
            @PathVariable UUID campaignId, @PathVariable UUID targetId) {
        geoTargetService.remove(campaignId, targetId);
        return ResponseEntity.noContent().build();
    }
}
