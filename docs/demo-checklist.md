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
- Open the `Marketplace` tab and claim an available open shift.
- Open the `Assistant` tab and review weekly hours, estimated earnings, and coverage gaps.

## Technical Talking Points

- Conflict prevention is implemented in `ValidateTradeEligibilityUseCase`.
- Weekly hour and earnings metrics are centralized in `CalculateWeeklyScheduleMetricsUseCase`.
- `ScheduleRepository` keeps presentation code independent from Room, Retrofit, and AI service details.
- `InMemoryScheduleRepository` makes the sample demoable without backend credentials.
- `OfflineFirstScheduleRepository`, Room entities, and Retrofit DTOs show the production-ready extension path.

## Test Talking Points

- Unit tests cover conflicting shifts, max weekly hours, valid claims, swap request routing, and weekly metrics.
- Compose UI test coverage verifies the swap request dialog flow.
- The GitHub Actions check validates project structure, documentation signals, and test-file coverage.
