#!/usr/bin/env bash
set -euo pipefail

required_files=(
  "settings.gradle.kts"
  "build.gradle.kts"
  "app/build.gradle.kts"
  "app/src/main/AndroidManifest.xml"
  "app/src/main/java/com/shiftsync/app/MainActivity.kt"
  "app/src/main/java/com/shiftsync/app/domain/usecase/ValidateTradeEligibilityUseCase.kt"
  "app/src/main/java/com/shiftsync/app/domain/usecase/CalculateWeeklyScheduleMetricsUseCase.kt"
  "app/src/test/java/com/shiftsync/app/domain/usecase/ValidateTradeEligibilityUseCaseTest.kt"
  "app/src/test/java/com/shiftsync/app/domain/usecase/CalculateWeeklyScheduleMetricsUseCaseTest.kt"
  "README.md"
  "docs/demo-checklist.md"
  "docs/adr/0001-offline-first-scheduling.md"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file"
    exit 1
  fi
done

grep -q "Clean Architecture" README.md
grep -q "Jetpack Compose" README.md
grep -q "AI Workflow Transparency" README.md
grep -q "Testing" README.md
grep -q "ShiftSync Demo Checklist" docs/demo-checklist.md
grep -q "Offline-First Scheduling Boundary" docs/adr/0001-offline-first-scheduling.md

if grep -R "TODO\\|FIXME" app/src README.md; then
  echo "Found unfinished TODO/FIXME markers."
  exit 1
fi

main_kotlin_count=$(find app/src/main/java -name "*.kt" | wc -l | tr -d " ")
test_kotlin_count=$(find app/src/test app/src/androidTest -name "*.kt" | wc -l | tr -d " ")

if [[ "$main_kotlin_count" -lt 25 ]]; then
  echo "Expected at least 25 main Kotlin files, found $main_kotlin_count."
  exit 1
fi

if [[ "$test_kotlin_count" -lt 3 ]]; then
  echo "Expected at least 3 Kotlin test files, found $test_kotlin_count."
  exit 1
fi

echo "ShiftSync project validation passed."
