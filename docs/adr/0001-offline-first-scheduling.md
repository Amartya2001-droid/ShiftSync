# ADR 0001: Offline-First Scheduling Boundary

## Status

Accepted

## Context

Restaurant staff often check schedules between service periods, on unreliable Wi-Fi, or while moving between stations. A scheduling app should load the current roster quickly and still make core state visible when the network is slow.

ShiftSync also includes AI-assisted notes and schedule insights. Those features should help communication, but they should not own the source of truth for shifts, claims, or eligibility rules.

## Decision

ShiftSync uses `ScheduleRepository` as the single presentation-facing data contract.

- Room is the active on-device source of truth for shifts, employees, and trade requests.
- Retrofit DTOs model the backend integration path.
- Domain use cases own scheduling rules, metrics, and note drafting.
- The app uses `OfflineFirstScheduleRepository` with an application-scoped database and a one-time, transactional sample seed. The employee table is the initialization marker; startup never resets an empty open board.
- Shift updates and trade-request inserts share a transaction. Claims recheck availability inside the transaction so a stale request cannot overwrite a prior claimant.
- `InMemoryScheduleRepository` remains a test fixture. No remote sync is implemented.
- AI-style note drafting is exposed behind use cases and repository methods rather than embedded in Compose screens.

## Consequences

- Compose screens stay focused on state rendering and events.
- Business rules can be tested without Android dependencies.
- Local mutations survive process restarts. Clearing app storage removes them.
- The sample schedule keeps its original seeded dates; it is not silently replaced each week.
- A real backend and sync/conflict protocol remain future work. Existing Retrofit declarations are not a working service.
- AI providers can change without leaking provider-specific prompts into presentation code.
