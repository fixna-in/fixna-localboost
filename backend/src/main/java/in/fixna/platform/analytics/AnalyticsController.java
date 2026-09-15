package in.fixna.platform.analytics;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.analytics.dto.CampaignMetricsResponse;
import in.fixna.platform.analytics.dto.DashboardResponse;
import in.fixna.platform.analytics.dto.MetricUpsertRequest;
import in.fixna.platform.analytics.dto.SeedResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Analytics endpoints. Scope always derives from the JWT principal;
 * windows default to the last 30 days and are inclusive.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "analytics", description = "Dashboard, campaign metrics and demo seeding")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "Tenant dashboard: totals, per-campaign rollup, lead funnel")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(analyticsService.dashboard(from, to));
    }

    @Operation(summary = "Campaign metrics timeline + totals (tenant-scoped)")
    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<CampaignMetricsResponse> campaignMetrics(
            @PathVariable UUID campaignId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(analyticsService.campaignMetrics(campaignId, from, to));
    }

    @Operation(summary = "Upsert one day of metrics for a campaign (write roles)")
    @PostMapping("/campaigns/{campaignId}/metrics")
    public ResponseEntity<Void> ingest(
            @PathVariable UUID campaignId, @Valid @RequestBody MetricUpsertRequest request) {
        analyticsService.ingest(campaignId, request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Seed deterministic demo metrics (write roles)")
    @PostMapping("/demo-seed")
    public ResponseEntity<SeedResult> seedDemo(@RequestParam(required = false) UUID campaignId) {
        return ResponseEntity.ok(analyticsService.seedDemo(campaignId));
    }
}
