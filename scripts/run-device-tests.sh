#!/usr/bin/env bash
set -uo pipefail
mkdir -p app/build/device-evidence
adb devices -l | tee app/build/device-evidence/adb-devices.txt
adb shell getprop ro.build.version.sdk > app/build/device-evidence/api-level.txt
adb shell getprop ro.product.cpu.abi > app/build/device-evidence/abi.txt
adb logcat -c
gradle --no-daemon connectedDebugAndroidTest 2>&1 | tee app/build/device-evidence/connected-tests.log
test_status=${PIPESTATUS[0]}
adb logcat -d > app/build/device-evidence/logcat.txt
adb pull /sdcard/Pictures/shiftsync-ui-evidence app/build/device-evidence/
screenshot_status=$?
if [[ "$test_status" -ne 0 ]]; then
  adb shell screencap -p /sdcard/instrumentation-failure.png
  adb pull /sdcard/instrumentation-failure.png app/build/device-evidence/ || true
fi
python3 scripts/summarize-test-results.py app/build/outputs/androidTest-results/connected > app/build/device-evidence/test-counts.txt
summary_status=$?
cat app/build/device-evidence/test-counts.txt
if [[ "$test_status" -ne 0 ]]; then exit "$test_status"; fi
if [[ "$summary_status" -ne 0 ]]; then exit "$summary_status"; fi
if [[ "$screenshot_status" -ne 0 ]]; then echo 'Walkthrough screenshot collection failed'; exit "$screenshot_status"; fi
python3 - <<'PYIMAGES'
from pathlib import Path
files = sorted(Path('app/build/device-evidence/shiftsync-ui-evidence').glob('*.png'))
print(f'DEVICE_SCREENSHOTS: {len(files)}')
for file in files:
    print(file.name)
if len(files) < 8:
    raise SystemExit('Missing walkthrough screenshots')
PYIMAGES
