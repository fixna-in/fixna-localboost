# Workflow 07 — Platform Adapters — State

## Status: COMPLETE ✅
Verdict: `mvn -f backend/pom.xml test` → **Tests run: 87, Failures: 0,
Errors: 0, Skipped: 0 — BUILD SUCCESS** (marker TEST_OK, 2026-09-11 14:06 IST).
New suites green: MockPlatformAdapterTest (7), CampaignLaunchServiceTest (7),
PlatformConnectionServiceTest (7).

## What shipped
- platform/ module (ADR-004: SDK-free, adapters only): AdvertisingPlatformAdapter,
  PlatformLaunchRequest, LaunchReceipt, PlatformException,
  AbstractMockPlatformAdapter (deterministic, idempotent by externalReference),
  MockGoogleAdsAdapter, MockMetaAdsAdapter, MockWhatsAppAdapter,
  PlatformAdapterRegistry (GOOGLE/META/WHATSAPP), PlatformConnection (V5
  platform_connections), PlatformConnectionRepository, dto/ (token columns
  never exposed), PlatformConnectionService (tenant-scoped, requireWrite,
  null-safe entityRef for audit), PlatformConnectionController
  (/api/v1/platforms/connections).
- campaign/ launch orchestration (no DB txn held across adapter calls):
  CampaignLaunchTx (transactional begin/applyOutcome; QUEUED→CREATING→
  ACTIVE/FAILED; CREATING→FAILED recoverable), CampaignLaunchService
  (non-txn executor, thread-safe Attempt record), CampaignLaunchController
  (POST /api/v1/campaigns/{id}/launch).
- Docs: docs/03-api/platform.md.

## Fix of record
PlatformConnectionServiceTest NPEs (connect/disconnect): JPA @PrePersist does
not run against mocked repos, so id was null in `getId().toString()` audit
refs → added private `entityRef()` (returns "new" when id null), used at
connect/disconnect audit events. Production behavior unchanged.

## Next
Workflow 08 — analytics + leads (.cline/workflows/08-analytics-leads.md).
