# Current Task

Task: Production track — OpenTelemetry + CI hardening
Phase: production
Status: IN_PROGRESS

## Completed this session
- [x] Fixed compile error (`PlatformException.getPlatform()` in launch logging).
- [x] Fixed Log4j2 test config (INFO levels for `fixna.access`/`fixna.telemetry`,
  `%ex{short}` pattern).
- [x] Removed duplicate `Application.java` (canonical main: `FixnaApplication`).
- [x] Added Micrometer OTel bridge + OTLP exporter dependencies.
- [x] Wired trace/span ids into MDC via `RequestIdFilter`.
- [x] Profile-based OTLP export config (off local/test, on dev/staging/prod).
- [x] CI frontend job switched to `npm ci` + lock-file cache.
- [x] Backend tests green (200 run, 6 skipped); frontend build green.

## Next
- [ ] CI: Docker image build for backend
- [ ] CI: test coverage reporting
- [ ] CI: container image security scan
- [ ] Deployment workflow / infrastructure docs
- [ ] Wire Vitest for frontend unit tests
