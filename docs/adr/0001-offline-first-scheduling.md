# ADR 0001: Offline-First Scheduling Boundary

## Status

Accepted

## Context

Restaurant staff often check schedules between service periods, on unreliable Wi-Fi, or while moving between stations. A scheduling app should load the current roster quickly and still make core state visible when the network is slow.

ShiftSync also includes AI-assisted notes and schedule insights. Those features should help communication, but they should not own the source of truth for shifts, claims, or eligibility rules.

## Decision

ShiftSync uses `ScheduleRepository` as the single presentation-facing data contract.

- Room entities model the offline cache path.
- Retrofit DTOs model the backend integration path.
- Domain use cases own scheduling rules, metrics, and note drafting.
- The sample app uses `InMemoryScheduleRepository` so it can launch without credentials.
- AI-style note drafting is exposed behind use cases and repository methods rather than embedded in Compose screens.

## Consequences

- Compose screens stay focused on state rendering and events.
- Business rules can be tested without Android dependencies.
- The app can later replace seeded data with Room plus a real backend without rewriting the UI.
- AI providers can change without leaking provider-specific prompts into presentation code.
