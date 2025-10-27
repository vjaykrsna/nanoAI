# Testing & Coverage Guide

Use this guide when you need to run nanoAI’s automated checks, add new tests, or understand how coverage is enforced. It condenses every workflow into a single reference so new contributors can get productive without wading through multiple documents.

## 1. What We Optimise For
- **Layered Kotlin architecture**: UI (Compose), domain (UseCases/ViewModels), and data (Room/Retrofit) stay decoupled and are tested at their boundaries.
- **Accessibility + offline parity**: UI tests mirror Material 3 semantics, TalkBack output, and offline fallbacks so no feature ships without assistive coverage.
- **Deterministic async code**: Coroutine code runs under `kotlinx.coroutines.test` with explicit dispatchers (`MainDispatcherExtension`) to keep flakes out of CI.
- **Coverage gates as the guardrail**: CI fails if coverage drops below 75 % (ViewModel), 65 % (UI), or 70 % (Data). Reports feed the risk register so leadership signs off with evidence.
- **JUnit 5 throughout**: All tests use JUnit 5 (Jupiter) for consistent annotations and modern testing features.

| Layer | Target | Measured Via |
| --- | --- | --- |
| ViewModel / domain | ≥ 75 % | JaCoCo layer map → `verifyCoverageThresholds` |
| UI / Compose | ≥ 65 % | Managed + connected instrumentation coverage |
| Data / repositories | ≥ 70 % | JVM + instrumentation merged coverage |

### JUnit 5 Migration Complete ✅
The project has been fully migrated to JUnit 5 (Jupiter):
- ✅ `@Before` → `@BeforeEach`
- ✅ `@After` → `@AfterEach`
- ✅ `@get:Rule` → `@RegisterExtension` (with `@JvmStatic` for TestEnvironmentRule)
- ✅ JUnit Vintage Engine included for backward compatibility

## 2. Test Suite Matrix
| Suite | Path | Default Command | Notes |
| --- | --- | --- | --- |
| JVM unit + contract | `app/src/test/java` | `./gradlew testDebugUnitTest` | Supports `--tests "pkg.ClassTest"` selectors. Generates HTML in `app/build/reports/tests/testDebugUnitTest/` and JaCoCo `.exec` files. |
| Instrumentation (Compose UI + device flows) | `app/src/androidTest/java` | `./gradlew ciManagedDeviceDebugAndroidTest` | Boots the CI-managed Pixel 6 ATD image. For physical device testing pass `-Pnanoai.usePhysicalDevice=true`. |
| Macrobenchmark | `macrobenchmark/src/main` | `./gradlew :macrobenchmark:connectedCheck` | Runs only when a device/emulator is attached. CI gate is optional but results are published for performance budgets. |
| Coverage tooling | `scripts/coverage` | `./gradlew jacocoFullReport` | Merges JVM + instrumentation coverage, produces HTML + XML under `app/build/reports/jacoco/full/`. |

### Module-Specific Test Tasks
For focused development and faster feedback, run tests for specific layers using command line filters:

| Module | Command | Description |
| --- | --- | --- |
| Core | `./gradlew testDebugUnitTest --tests "*.core.*"` | Core utilities and base classes |
| Feature | `./gradlew testDebugUnitTest --tests "*.feature.*"` | Feature-specific logic and ViewModels |
| Model | `./gradlew testDebugUnitTest --tests "*.model.*"` | Data models and domain entities |
| Security | `./gradlew testDebugUnitTest --tests "*.security.*"` | Security-related utilities |
| UI | `./gradlew testDebugUnitTest --tests "*.ui.*"` | UI components and Compose logic |
| Data | `./gradlew testDebugUnitTest --tests "*.data.*"` | Repositories, DAOs, and data access |
| Domain | `./gradlew testDebugUnitTest --tests "*.domain.*"` | Use cases and business logic |

### Android Instrumentation Test Commands
For instrumentation tests (Android UI/device tests):
```bash
# Run all instrumentation tests (connectedAndroidTest)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="com.vjaykrsna.nanoai.feature.chat.ui.ChatScreenTest"

# Run with managed device
./gradlew ciManagedDeviceDebugAndroidTest

# Filter by test name
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="*ChatScreenTest*"
```

**Note**: Instrumentation tests use `AndroidJUnitRunner` and require a device or emulator. The `--tests` flag used for unit tests doesn't work for instrumentation tests.

**Note**: Custom Gradle tasks for each module can be added to `app/build.gradle.kts` if needed, but command line filters provide the same functionality with simpler configuration.

### Shared helpers
- `MainDispatcherExtension` overrides `Dispatchers.Main` for coroutine tests.
- `TestEnvironmentRule` resets Room/DataStore/network toggles between instrumentation runs (use as `@RegisterExtension` with `@JvmStatic` for JUnit 5).
- Fixture builders live under `app/src/test/java/com/vjaykrsna/nanoai/**/fixtures` and `DomainTestBuilders` simplifies thread/message creation.

### JUnit 5 Patterns
**Android Instrumentation Tests:**
```kotlin
@ExtendWith(TestEnvironmentRule::class)
class MyInstrumentationTest {
  
  companion object {
    @RegisterExtension
    @JvmStatic
    val testEnvironmentRule = TestEnvironmentRule() // Shared across all tests
  }
  
  @RegisterExtension
  val composeRule = createAndroidComposeRule<MainActivity>()
  
  @BeforeEach
  fun setup() { /* ... */ }
}
```

**Unit Tests:**
```kotlin
class MyUnitTest {
  @BeforeEach
  fun setup() { /* ... */ }
  
  @Test
  fun testSomething() { /* ... */ }
}
```

## 3. Everyday Runbook
1. **While coding** run targeted JVM tests: `./gradlew testDebugUnitTest --tests "*.YourTest"`.
2. **Before pushing** execute the fast lane:
   ```bash
   ./gradlew spotlessCheck detekt testDebugUnitTest
   ```
3. **Before opening a PR** (mirrors the CI `check` task):
   ```bash
   ./gradlew check
   ```
   `check` runs Spotless, Detekt, all JVM tests, managed-device instrumentation, merged coverage, and threshold verification.
4. **After large UI changes** run instrumentation locally:
   ```bash
   ./gradlew ciManagedDeviceDebugAndroidTest
   ```
   Use `-Pandroid.testInstrumentationRunnerArguments.class="*YourTest"` to target specific suites.

## 4. Coverage Pipeline
1. **Merge reports**
   ```bash
   ./gradlew jacocoFullReport
   ```
   - Uses emulator by default locally; pass `-Pnanoai.usePhysicalDevice=true` for physical device testing.
   - Pass `-Pnanoai.skipInstrumentation=true` when you only need JVM coverage locally (CI must keep instrumentation enabled).
2. **Enforce thresholds**
   ```bash
   ./gradlew verifyCoverageThresholds --report-xml app/build/reports/jacoco/full/jacocoFullReport.xml --json app/build/coverage/report.json
   ```
   Fails fast if any layer slips below its target and writes a summary to `app/build/coverage/thresholds.md`.
3. **Publish summaries**
   ```bash
   ./gradlew coverageMarkdownSummary
   ```
   Generates `app/build/coverage/summary.{md,json}` and an HTML mirror under `app/build/reports/jacoco/full/`.
4. **Bundle artefacts for CI uploads** *(optional locally, required for release pipelines)*
   ```bash
   ./gradlew coverageMergeArtifacts
   ```
   Packages HTML, XML, Markdown, JSON, and raw `.exec/.ec` inputs into `app/build/reports/jacoco/full/`.

### Quick artefact map
- `app/build/reports/tests/testDebugUnitTest/index.html` — JVM report
- `app/build/reports/androidTests/managedDebug/` — instrumentation report
- `app/build/reports/jacoco/full/index.html` — merged coverage dashboard
- `app/build/coverage/summary.md` — layer-by-layer digest consumed by the risk register
- `config/coverage/layer-map.json` — class → layer rules (update when adding packages)

## 5. Adding or Updating Tests
1. Start from the task list (`specs/005-improve-test-coverage/tasks.md`) and write the failing test first.
2. Choose the smallest suite that can prove the behaviour:
   - JVM: pure Kotlin, ViewModels, repositories, type converters.
   - Instrumentation: Composables, accessibility semantics, offline UX.
   - Macrobenchmark: startup, transitions, frame timing.
3. Inject dependencies through Hilt test components or fakes; never hit live services.
4. Use deterministic tools (`Clock.System`, `MainDispatcherExtension`, `StandardTestDispatcher`).
5. Document intent with concise KDoc and mention the related risk ID (see `docs/coverage/risk-register.md`).
6. Re-run `jacocoFullReport` + `verifyCoverageThresholds` before committing to catch regressions locally.

## 6. Troubleshooting Cheat Sheet
- **Physical device required**: Pass `-Pnanoai.usePhysicalDevice=true` to run tests on a physical device instead of the default emulator.
- **Instrumentation filtering**: Use runner args (`-Pandroid.testInstrumentationRunnerArguments.class="*Test"`). The standard `--tests` flag is ignored for device tasks.
- **Offline scenario flakes**: After toggling radios, clear state with `adb shell pm clear com.vjaykrsna.nanoai` and rerun with `TestEnvironmentRule` enabled.
- **Coverage gaps**: Open `app/build/reports/jacoco/full/index.html`, filter by package, then cross-check `app/build/coverage/summary.md` to see which layer failed.
- **Long-running managed runs**: Add `-Pandroid.testInstrumentationRunnerArguments.notAnnotation=flaky` to skip known unstable tests while a fix is in progress (and file a risk-register item).
- **Physical device sleeps**: Keep the screen awake with `adb shell svc power stayon true` during test runs; revert to `false` afterwards.

Pair this guide with `specs/005-improve-test-coverage/quickstart.md` when onboarding or rehearsing the full workflow end-to-end.
