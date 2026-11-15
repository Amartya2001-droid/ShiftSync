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
- Pending open-board requests become Accepted when a claim succeeds; superseded targeted requests become Rejected. The Requests tab displays participant-scoped history and status; named recipients can Accept/Decline pending targeted offers.
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

## Request History

- Send a targeted request as Maya, switch to its recipient, and open Requests: confirm the sender, note, shift, and Pending status.
- As the named recipient, Accept: confirm ownership transfers and both histories show Accepted. Repeat with another offer and Decline: confirm the sender retains ownership and status is Rejected.
- Sending a new offer for the same shift supersedes earlier pending offers.
- Unrelated demo employees should not see the request or be able to respond.
- Claim an open-board post and check that the saved Accepted status is visible to both participants after relaunch.

## Withdraw an Offer

- As Maya, send Sam a targeted offer; in Requests, choose Withdraw offer and confirm. Both histories should show Cancelled and Maya should retain the Confirmed shift. Sam can no longer accept.
- Post a shift to the open board, withdraw it, and confirm it disappears from Marketplace while the roster/hours remain with Maya. Coverage gaps should refresh.
- Keep offer in the confirmation dialog should leave the request unchanged.
- Try a stale withdrawal after acceptance/claim: it must fail without reversing ownership.
- This walkthrough still requires real-device verification; CI JVM/Robolectric results are reported separately.
