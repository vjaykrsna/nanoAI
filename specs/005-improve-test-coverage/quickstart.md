# Quickstart: Improve Test Coverage for nanoAI

Follow this checklist to validate the coverage initiative end-to-end.

## 1. Workspace Prep
- Checkout branch `005-improve-test-coverage`.
- Sync dependencies: `./gradlew spotlessApply` (optional) then `./gradlew help` to warm Gradle.
- **Emulator ABI Requirement**: The managed Pixel 6 API 34 configuration uses `x86_64` ABI (stabilized in T001). Ensure hardware virtualization is enabled (`egrep -c '(vmx|svm)' /proc/cpuinfo` > 0 on Linux) or use a physical device for instrumentation tests.
- Provision an Android emulator (API 31+, Play Services image) or physical device and verify you can toggle radios for offline checks (`adb shell svc wifi disable`, `adb shell svc data disable`).
- This branch completes the JUnit5 → JUnit6 migration; all JVM tests now run on the Jupiter platform with no fallback to vintage runners.
- **Test Environment Isolation**: All instrumentation tests use `TestEnvironmentRule` to reset DataStore/Room state and network toggles before each test, ensuring first-launch disclaimers and offline flows start from a clean slate.

## 2. Run Automated Suites (JUnit6)
- **ViewModel + Repository**: `./gradlew testDebugUnitTest`
  - Target specific Jupiter classes with `./gradlew testDebugUnitTest --tests "com.vjaykrsna.nanoai.coverage.*"`.
  - **Status**: ✓ Passing (verified in T022)
- **Compose UI (instrumented)**: Currently blocked by pre-existing compilation errors in androidTest code
  - **Expected command**: `./gradlew ciManagedDeviceDebugAndroidTest` (when compilation errors are fixed)
  - Spins up the managed Pixel 6 API 34 virtual device (AOSP ATD) headlessly—the same configuration CI uses.
  - For local fallbacks with a running emulator or physical hardware, run `./gradlew connectedDebugAndroidTest`; if you previously enabled the managed-device property, pass `-Pnanoai.useManagedDevice=false` to force the direct device-provider task.
  - Add `-Pandroid.testInstrumentationRunnerArguments.notAnnotation=flaky` when triaging flaky-tagged cases.
  - Confirms Material accessibility semantics and offline flows via MockWebServer.

## 3. Generate Coverage Reports
- Execute `./gradlew jacocoFullReport` (custom task) to merge unit + instrumentation coverage. On CI—or when passing `-Pnanoai.useManagedDevice=true`—the task bootstraps the managed Pixel 6 API 34 device automatically before running instrumentation tests.
  - When hardware virtualization is unavailable locally, append `-Pnanoai.skipInstrumentation=true` to skip the managed-device run and merge unit-test coverage only (CI must continue to run full instrumentation).
- Locate outputs:
  - HTML: `app/build/reports/jacoco/full/index.html`
  - XML: `app/build/reports/jacoco/full/jacocoFullReport.xml`

## 4. Validate Threshold Enforcement
- Run `./gradlew verifyCoverageThresholds`.
  - Command fails if any layer drops below ViewModel 75%, UI 65%, Data 70%.
  - Inspect console summary highlighting offending modules.

## 5. Review Risk Register
- Open `docs/coverage/risk-register.md` and confirm all High/Critical items have assigned owners & mitigation builds.
- Track the latest coverage snapshot and ensure any new risks discovered during testing are documented with severity, mitigation plans, and target builds.

## 6. Ensure New Principles Compliance
- Verify all new test code includes KDoc comments and has undergone peer review.
- Confirm coverage reports are published in CI artifacts with release notes documenting changes.

## 7. Update Stakeholders
- Publish coverage deltas in team channel with markdown snippet produced by the tooling (`/build/coverage/summary.md`).
- Record improvements + outstanding gaps in `docs/todo-next.md`.

## Troubleshooting
- **Offline instrumentation flakes**: Re-enable radios after tests (`adb shell svc wifi enable`, `adb shell svc data enable`) and clear cached state with `adb shell pm clear com.vjaykrsna.nanoai` so MockWebServer scenarios repopulate deterministically.
- If instrumentation tests fail due to emulator offline, rerun with `ANDROID_SERIAL` or mark for rerun while logging issue in risk register.
- Flaky test triage: label suites with `@FlakyTest` temporarily, open tracking ticket, and ensure coverage summary flags the reduced confidence.
