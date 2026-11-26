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
adb pull /sdcard/Android/data/com.shiftsync.app/files/ui-evidence app/build/device-evidence/ || true
if [[ "$test_status" -ne 0 ]]; then
  adb shell screencap -p /sdcard/instrumentation-failure.png
  adb pull /sdcard/instrumentation-failure.png app/build/device-evidence/ || true
fi
python3 scripts/summarize-test-results.py app/build/outputs/androidTest-results/connected > app/build/device-evidence/test-counts.txt
summary_status=$?
cat app/build/device-evidence/test-counts.txt
if [[ "$test_status" -ne 0 ]]; then exit "$test_status"; fi
exit "$summary_status"
