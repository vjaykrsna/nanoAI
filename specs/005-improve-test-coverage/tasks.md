# Tasks: JUnit4 to JUnit6 Migration (005-improve-test-coverage)

**Feature Branch**: `005-improve-test-coverage` | **Date**: 2025-10-26 | **Spec**: [/specs/005-improve-test-coverage/spec.md](spec.md)

## Overview
Migrate all remaining JUnit4 Android instrumentation tests to JUnit6-compatible runners to complete the testing modernization. Unit tests already run on JUnit6, but UI/data layer integration tests need runner updates for improved nullability, CSV support, and overall ecosystem alignment.

**User Stories Mapped**:
- **US1**: Modernize instrumentation test infrastructure (runners, annotations)
- **US2**: Update UI layer tests for JUnit6 compatibility
- **US3**: Migrate data layer integration tests
- **US4**: Ensure macrobenchmark and performance tests work with JUnit6

## Implementation Strategy
- **MVP Scope**: Complete US1 (infrastructure) first to unblock US2-US4
- **Incremental Delivery**: Migrate one test category at a time to maintain CI green
- **Parallel Execution**: Mark independent test file updates with [P]
- **Testing Approach**: TDD - each migration batch validated by running full test suite

---

## Phase 1: Setup (Infrastructure Preparation)

- [X] T001 Update JUnit dependencies in gradle/libs.versions.toml for AndroidJUnit6 compatibility
- [X] T002 Update app/build.gradle.kts to use modern junit4-free libraries
- [X] T003 Verify Java 17/Kotlin 2.2 baselines support JUnit6 features
- [X] T004 Confirm CI configuration for JUnit6 reporting and execution

## Phase 2: Foundational (Core Migration Infrastructure)

- [X] T005 [P] Create AndroidJUnit6 test runner configuration in TestEnvironmentRule
- [X] T006 [P] Update ComposeTestHarness.kt for JUnit6 compatibility
- [X] T007 [P] Update BaseModelLibraryScreenTest.kt to use modern JUnit6 annotations
- [X] T008 [P] Update BaseSidebarContentTest.kt runner annotations
- [X] T009 Test foundational changes with baseline instrumentation suite

---

## Phase 3: US1 - Instrumentation Test Infrastructure Migration

- [X] T010 [US1] Migrate ExampleInstrumentedTest.kt to AndroidJUnit6 runner
- [X] T011 [US1] Update all @RunWith(AndroidJUnit4::class) annotations across instrumentation tests
- [X] T012 [US1] [P] Replace androidx.compose.ui.test.junit4 imports with modern alternatives
- [X] T013 [US1] [P] Update AndroidJUnit4 imports to use newer test runners
- [ ] T014 [US1] Validate Phase 3 changes with ./gradlew ciManagedDeviceDebugAndroidTest

---

## Phase 4: US2 - UI Layer Test Migration

**UI Tests to Migrate (19 files)**:
- All files matching app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/
- All files matching app/src/androidTest/java/com/vjaykrsna/nanoai/shared/ui/
- All files matching app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/ui/
- And more from search results

- [X] T015 [US2] [P] Migrate CoverageDashboardTest.kt from AndroidJUnit4 to compatible runner
- [X] T016 [US2] [P] Migrate DisclaimerDialogTest.kt annotations and imports
- [X] T017 [US2] [P] Migrate all HomeHubFlowTest.kt and CommandPaletteComposeTest.kt UI tests
- [X] T018 [US2] [P] Update feature/uiux test files for JUnit6 runner compatibility
- [X] T019 [US2] [P] Migrate feature/library screen tests (ModelLibraryScreenStructureTest.kt, etc.)
- [X] T020 [US2] [P] Update settings UI tests (SettingsScreenBackupRestoreTest.kt, etc.)
- [X] T021 [US2] [P] Migrate navigation tests (HomeNavigationScenarioTest.kt, SidebarNavigationTest.kt)
- [X] T022 [US2] Verify all UI layer tests pass with ./gradlew connectedDebugAndroidTest

---

## Phase 5: US3 - Data Layer Test Migration

**Data Tests to Migrate (3 files)**:
- All DAO and repository instrumentation tests in app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/data/
- All DAO and repository instrumentation tests in app/src/androidTest/java/com/vjaykrsna/nanoai/feature/settings/

- [X] T023 [US3] [P] Migrate DownloadTaskDaoTest.kt to AndroidJUnit6
- [X] T024 [US3] [P] Migrate ModelCatalogRepositoryImplTest.kt runner annotations
- [X] T025 [US3] [P] Migrate ModelPackageDaoTest.kt and related data integration tests
- [X] T026 [US3] Validate data layer tests maintain offline simulation capabilities

---

## Phase 6: US4 - Macrobenchmark Test Migration

**Macrobenchmark Tests to Migrate (6 files)**:
- All files in macrobenchmark/src/main/java/com/vjaykrsna/nanoai/

- [X] T027 [US4] [P] Migrate ColdStartBenchmark.kt to JUnit6-compatible runner
- [X] T028 [US4] [P] Migrate BaselineProfileGenerator.kt annotations
- [X] T029 [US4] [P] Update UiUxBaselineProfile.kt and UiUxStartupBenchmark.kt
- [X] T030 [US4] [P] Migrate CoverageDashboardStartupBenchmark.kt
- [X] T031 [US4] [P] Migrate NavigationBenchmarks.kt from AndroidJUnit4
- [X] T032 [US4] Verify macrobenchmark execution with ./gradlew macrobenchmark:coldStartBenchmark

---

## Final Phase: Polish & Validation

- [ ] T033 Ensure all @RunWith annotations removed across codebase
- [ ] T034 Update test reporting for JUnit6 features (CSV, nullability)
- [ ] T035 Verify no JUnit4 imports remain in test source sets
- [ ] T036 Run full test suite and confirm coverage thresholds maintained
- [ ] T037 Update docs/junit.md with migration completion notes
- [ ] T038 Execute integration tests on CI with JUnit6 environment

---

## Phase 7: US5 - Coverage Reporting Implementation

**FR-001 Coverage Summary, FR-005 Trend Data, FR-006 Thresholds**

- [X] T039 Configure JaCoCo in app/build.gradle.kts with layer-specific source sets
- [X] T040 Create coverage.gradle.kts task for consolidated ViewModel/UI/Data reports
- [X] T041 Implement coverage summary JSON schema validation in contracts
- [X] T042 Add CI step to generate coverage summary after unit+instrumentation builds
- [X] T043 Publish coverage reports with trend data visualization
- [X] T044 Implement threshold enforcement script for ViewModel 75%, UI 65%, Data 70%
- [X] T045 Add risk register tracking for coverage gaps below thresholds
- [X] T046 Validate coverage reports highlight changes and gaps appropriately

---

## Phase 8: US6 - ViewModel Coverage Expansion

**FR-002 Critical ViewModel Transitions**

- [x] T047 Audit ViewModel state transitions referenced in docs/todo-next.md
- [x] T048 [P] Implement unit tests for ChatViewModel happy path scenarios
- [x] T049 [P] Implement unit tests for MessageComposerViewModel error handling
- [x] T050 [P] Implement unit tests for HistoryViewModel loading states
- [x] T051 [P] Implement unit tests for ModelLibraryViewModel state transitions
- [x] T052 [P] Add deterministic mocks for coroutine flows and AI inference

---

## Phase 9: US7 - UI Coverage Expansion

**FR-003 Compose UI Flows**

- [ ] T053 Audit UI flows for coverage gaps (conversation list, chat detail, message composition)
- [ ] T054 [P] Expand instrumentation tests for chat thread list UI flow
- [ ] T055 [P] Expand instrumentation tests for message composition with accessibility
- [ ] T056 [P] Add instrumented tests for model library screen filtering and interaction
- [ ] T057 [P] Add instrumented tests for settings UI provider management
- [ ] T058 [P] Include Material design compliance assertions in UI tests

---

## Phase 10: US8 - Data Layer Coverage Expansion

**FR-004 Data Access Paths**

- [ ] T059 Audit data paths for Room DAOs, repositories, caching rules
- [ ] T060 [P] Implement DAO tests for read/write operations with offline simulation
- [ ] T061 [P] Implement repository tests for error propagation and caching
- [ ] T062 [P] Add integration tests for data integrity across network failures
- [ ] T063 [P] Implement data migration and backup testing scenarios

---

## Dependencies
```
US1 (Infrastructure) → Unblocks US2, US3, US4
US2 (UI) depends on US1, independent of US3, US4
US3 (Data) depends on US1, independent of US2, US4
US4 (Macrobenchmark) depends on US1, independent of US2, US3
Polish phase requires all US1-US4 complete
```

## Parallel Execution Opportunities
- Mark [P]: Independent file updates (different test classes, no shared dependencies)
- US2, US3, US4 can execute in parallel after US1 completion
- Within each US phase: Marked [P] tasks can run concurrently

## Independent Test Criteria
- **US1**: All test infrastructure builds without deprecated JUnit4 references
- **US2**: All UI tests execute on JUnit6 runners without annotation changes needed
- **US3**: Data integration tests pass offline scenarios with new runners
- **US4**: Macrobenchmarks produce valid performance reports

---

**Total Tasks**: 38 | **Parallel Eligible**: 26 | **User Stories**: 4
*Format Validation*: All tasks follow required - [ ] [TaskID] [P?] [Story?] filename format
*Suggested MVP*: Complete US1 to enable all other migrations
