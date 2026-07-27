# Contributing to ShiftSync

ShiftSync is intentionally structured as a reviewable Android portfolio project. Contributions should keep product behavior, architecture boundaries, and testing evidence easy to understand.

## Before Opening a PR

- Keep UI work in `presentation` and avoid putting business rules in composables.
- Put scheduling decisions in `domain/usecase` with focused unit tests.
- Keep data-source details behind `ScheduleRepository`.
- Update docs when a change affects architecture, AI behavior, demo flow, or testing.
- Run `bash scripts/validate-project.sh` before pushing.

## Commit Style

Use short imperative commit messages, such as:

- `Add roster status summary`
- `Prevent tight shift turnarounds`
- `Document offline-first scheduling boundary`

## AI Transparency

If AI assists with implementation, describe what it helped with and what was manually reviewed. Do not commit provider secrets, API keys, or personal schedule data.
