# WF08 State — Analytics & Leads

## Status: COMPLETE — BUILD SUCCESS, Tests run: 104, Failures: 0, Errors: 0, Skipped: 0
(wf08t2.log verdict extract via findstr; exit-1 was known harness noise)
- History: run #1 missing CampaignRepository import (fixed line 23); run #2
  seedDemo stub "CAMPAIGN_NOT_FOUND" (fixed — ownership stub line 178);
  run #3 green. 87 (WF07) + 8 AnalyticsServiceTest + 9 LeadServiceTest = 104.


## Artifacts landed
- `analytics/`: CampaignMetric (V5 `campaign_metrics`, upsert key campaign+metricDate), repo
  (`findByTenantIdAndCampaignIdAndMetricDate`, both `...Between...Asc`), 6 DTOs
  (DashboardResponse(from,to,totals,campaigns,leads) with CampaignRollup/MetricTotals/LedFunnel
  — funnel component is `leads`, MetricUpsertRequest(metricDate,spend<long primitives>,...),
  CampaignMetricsResponse.of(...), MetricPoint, SeedResult(campaigns,metricDays)),
  AnalyticsService (dashboard funnel+rollups, campaignMetrics timeline, ingest upsert
  overwrite, deterministic seedDemo via seeded Random, helpers add/sum/campaignName),
  AnalyticsController (`/api/v1/analytics`: GET dashboard, GET campaigns/{id}/metrics,
  POST campaigns/{id}/metrics ingest, POST seed).
- `lead/`: LeadStatus(NEW/CONTACTED/QUALIFIED/CONVERTED/LOST), Lead (V5 `leads`),
  tenant-scoped repo, LeadService (create/update/status/search paginated clamped,
  parent business/campaign ownership), LeadController (nested `/businesses/{id}/leads`),
  4 DTOs.
- Tests: LeadServiceTest (9 tests, complete), AnalyticsServiceTest (complete — part1
  defects `funnel()`→`leads` + undefined helpers fixed in part2).
- Docs: `docs/03-api/analytics-leads.md`.

## Verdict procedure (kill-proof)
1. Foreground `cmd /c "mvn -f backend\pom.xml test > wf08t.log 2>&1"` — harness reports
   exit code even when output is unobservable. Exit 0 = green (do NOT read log).
2. If exit 1: read wf08t.log ONCE — surefire summary names the exact test/line.
3. Never launch Start-Process detached then another terminal command — the second kills
   the first (the `^C` seen in this log).

## After green
- Closeout: BACKLOG.md mark analytics+leads, progress.md WF08 entry + test status,
  wf08-state.md → COMPLETE, CURRENT-TASK.md → next workflow (09 or backlog review),
  delete stale artifacts (check-verdict.ps1, wf08t.log, anst.mid if still present).

## Known-good baseline
WF07 verdict: 87 tests, 0 failures (wf07 closeout). WF08 adds AnalyticsServiceTest (7)
+ LeadServiceTest (9) → expect 103.
