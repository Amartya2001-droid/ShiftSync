# ShiftSync Demo Checklist

Use this checklist when walking a reviewer through the project.

## Repository Review

- Confirm the `main` branch has a green `ShiftSync CI` status check.
- Open `README.md` and point to the architecture diagram, AI workflow transparency, testing strategy, and tradeoff sections.
- Show the domain use cases under `app/src/main/java/com/shiftsync/app/domain/usecase`.

## Product Walkthrough

- Launch the app from Android Studio.
- Review the `Roster` tab and confirm assigned shifts show role, section, time, and status.
- Tap `Request Swap` on a shift and confirm the swap dialog appears.
- Enter a reason, choose `Open board` or a teammate, and submit the smart note.
- Open `Marketplace`: the posted shift shows its owner and cannot be claimed by its own poster.
- Use **Demo employee** to switch to Sam. Claim Maya’s posted shift; verify Sam’s roster gains it.
- Switch back to Maya; confirm her roster loses the shift and her hours decrease.
- The selector is for a local demo, not login/authentication. Selection resets to Maya on process restart.
- Open the `Assistant` tab and review weekly hours, estimated earnings, and coverage gaps.
- Force-stop and relaunch: confirm the claim and open-board post remain and insights match the saved schedule. Clearing app storage explicitly resets the sample.

## Technical Talking Points

- Conflict prevention is implemented in `ValidateTradeEligibilityUseCase` and checked inside the repository ownership-transfer transaction. Competing claims recheck saved availability and the claimant’s latest schedule.
- Pending open-board requests become Accepted when a claim succeeds; superseded targeted requests become Rejected. Request history and targeted acceptance UI are not implemented yet.
- Weekly hour and earnings metrics are centralized in `CalculateWeeklyScheduleMetricsUseCase`.
- `ScheduleRepository` keeps presentation code independent from Room, Retrofit, and AI service details.
- `OfflineFirstScheduleRepository` persists the usable local sample without backend credentials.
- Room transactions keep shift changes and request records together; a failed request insert rolls back the shift update.
- Retrofit DTOs remain an unconnected backend boundary; there is no multi-device sync.

## Test Talking Points

- Unit tests cover conflicting shifts, max weekly hours, valid claims, swap request routing, and weekly metrics.
- Compose UI test coverage verifies the swap request dialog flow.
- GitHub Actions validates project structure, builds the APK, executes JVM tests including file-backed Room/Robolectric persistence tests, and compiles instrumentation tests.
- Device instrumentation and the manual lifecycle walkthrough must be reported separately from JVM results.
