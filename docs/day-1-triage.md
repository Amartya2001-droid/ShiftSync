# Day 1 triage — ShiftSync

Program brief: November 11, 2026. Execution began November 10, 2026 (America/Halifax).
Baseline: `56676a2` (September 30 coverage-gap insights). No pre-existing open issues or PRs were returned during triage.

## Findings and fixes

- Android compilation failed because Java targeted 1.8 while Kotlin targeted 17. Set Java source/target compatibility to 17.
- kotlinx.serialization 1.7.3 requires Kotlin 2; the project uses Kotlin 1.9.25. Use serialization 1.6.3, retaining the existing Kotlin/Compose compiler pairing.
- CI only ran structural validation. Added APK assembly, JVM test execution, instrumentation-test APK compilation, manual dispatch, and test-report artifacts. The first branch run failed when the Android setup action requested the obsolete SDK `tools` package; explicit SDK packages fix setup.
- Coverage summaries were computed from current repository data but the ViewModel retained old insights after schedule mutations. Refresh insights when observed schedule state changes. The regression test exercises the actual seeded repository, claim/swap use cases, and ViewModel: initial gaps, removal of a claimed Host gap, updated hours, and reappearance after posting to the open board.

## Verification and limits

- Structural validation: passed.
- Local `assembleDebug testDebugUnitTest assembleDebugAndroidTest`: passed; 26 JVM tests, zero failures/errors. Debug app and instrumentation APKs compile.
- The environment has Android SDK 35 and Gradle 8.9. Its default Java install is incomplete; local verification used a scratch Temurin JDK 17 with the environment's Java CA trust store, in-process Kotlin compilation, and a writable Android user directory. No environment workaround was added to repository build settings.
- Traced Assistant tab → `ScheduleUiState.insight` → ViewModel → `GetScheduleInsightsUseCase` → seeded repository → `SummarizeOpenCoverageUseCase`, and tested the path through ViewModel state. Existing summarizer tests check grouping, role formatting, and exclusion of covered shifts.
- Device end-to-end verification and `connectedDebugAndroidTest` were not run: `adb devices` is empty and `/dev/kvm` is absent. Rendering, taps, and real-device lifecycle behavior remain unverified. Compilation of instrumentation tests is not a device test pass.
- Room/Retrofit integration is not wired into the demo, so no production backend or persistence end-to-end result is claimed.
- Not live yet — blocked by release preparation and distribution; no verified live URL or public app listing.

AI assistance prepared the triage fixes, documentation, workflow, and regression test; source review and actual build/test results informed this report. No new feature was added.
