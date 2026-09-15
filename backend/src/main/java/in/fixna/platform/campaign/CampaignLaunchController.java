package in.fixna.platform.campaign;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.campaign.dto.CampaignResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Executes a queued launch through the platform adapters. RBAC and state
 * rules live in the services; the controller only translates HTTP.
 */
@RestController
@RequestMapping("/api/v1/campaigns")
@Tag(name = "campaigns", description = "Campaign launch execution")
public class CampaignLaunchController {

    private final CampaignLaunchService launchService;

    public CampaignLaunchController(CampaignLaunchService launchService) {
        this.launchService = launchService;
    }

    @Operation(summary = "Execute queued launch (QUEUED→CREATING→ACTIVE/FAILED, idempotent)")
    @PostMapping("/{id}/execute-launch")
    public ResponseEntity<CampaignResponse> executeLaunch(@PathVariable UUID id) {
        return ResponseEntity.ok(launchService.run(id));
    }
}
