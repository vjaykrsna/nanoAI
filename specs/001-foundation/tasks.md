---

description: "Tasks for 001-foundation feature"

---

# Tasks: Offline Multimodal nanoAI Assistant

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 [P] Document current prerequisite results from .specify/scripts/bash/check-prerequisites.sh in specs/001-foundation/research.md (capture FEATURE_DIR, AVAILABLE_DOCS)
- [X] T002 [P] Align feature scope with AGENTS.md and docs/architecture/ARCHITECTURE.md; note any conflicts in specs/001-foundation/research.md
- [X] T003 [P] Inventory current coverage gates and CI commands in config/testing/coverage and config/quality/quality-gates.json; summarize gating steps in specs/001-foundation/research.md

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Audit chat persistence for plaintext storage by reviewing app/src/main/java/com/vjaykrsna/nanoai/core/data/db/entities/MessageEntity.kt and related DAOs to confirm text field handling; record findings in specs/001-foundation/research.md
- [ ] T005 Assess encryption mechanisms available (EncryptedSecretStore, Room support) in app/src/main/java/com/vjaykrsna/nanoai/core/security and app/src/main/java/com/vjaykrsna/nanoai/core/data/db; propose options in specs/001-foundation/research.md (depends on T004)
- [ ] T006 Define remediation plan for encrypted message storage and migration in specs/001-foundation/plan.md and data-model alignment in specs/001-foundation/data-model.md (depends on T005)
- [ ] T007 [P] Profile build pipeline to validate over-engineering claim: measure clean build and incremental build times, attribute plugin/config impacts from build-logic/ and app/build.gradle.kts, and propose actions to reach <2 min local build; log bottlenecks and decisions in specs/001-foundation/research.md
- [ ] T008 [P] Map use-case/repository indirections (e.g., ChatFeatureCoordinator, thin GetDefaultPersonaUseCase) across app/src/main/java/com/vjaykrsna/nanoai/feature/chat and core/domain to quantify redundancy; summarize counts, candidate merges, and layer-preserving options in specs/001-foundation/research.md
- [ ] T009 Establish migration test scaffolding for encrypted chat storage in app/src/test/java/com/vjaykrsna/nanoai/core/data/db/MessageDaoMigrationTest.kt (failing test that expects ciphertext) (depends on T006)
- [ ] T010 Update Room schema and migration plan documents in specs/001-foundation/contracts/model-manifest.json notes and docs/architecture/ARCHITECTURE.md section references (depends on T006)
- [ ] T011 [P] Quantify thin use cases/coordinator redundancy (sample 200+ files) and propose consolidation that retains ViewModel → UseCase → Repository → DataSource flow; document keep/delete/merge rationale in specs/001-foundation/research.md (depends on T008)
- [ ] T012 Add architecture guard: checklist in specs/001-foundation/plan.md to prevent direct ViewModel → Repository shortcuts during refactors; require code review gate noted in specs/001-foundation/research.md (depends on T011)
- [ ] T013 [P] Validate export/import warning behavior and data classification (Secrets vs Sensitive) across settings and chat flows; add gaps and required warnings to specs/001-foundation/research.md (depends on T006)
- [ ] T014 [P] Validate credential encryption and decryption paths with tests in app/src/test/java/com/vjaykrsna/nanoai/core/security/EncryptedSecretStoreTest.kt and app/src/test/java/com/vjaykrsna/nanoai/core/data/datastore/CredentialStoreTest.kt (depends on T005)

## Phase 3: User Story 1 - Local-first Private Assistant (Priority: P1)

### Tests
- [ ] T015 [P] [US1] Add offline chat integration test ensuring local inference with connectivity loss in app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/ui/ChatScreenOfflineTest.kt (fails until wiring complete)
- [ ] T016 [P] [US1] Add unit test covering encrypted message persistence path in app/src/test/java/com/vjaykrsna/nanoai/core/data/db/MessageDaoTest.kt (depends on T009)

### Implementation
- [ ] T017 [US1] Implement encrypted text field for MessageEntity in app/src/main/java/com/vjaykrsna/nanoai/core/data/db/entities/MessageEntity.kt with migration in app/src/main/java/com/vjaykrsna/nanoai/core/data/db/migrations (depends on T009)
- [ ] T018 [US1] Wire encryption/decryption in MessageDao and repository layer app/src/main/java/com/vjaykrsna/nanoai/core/data/db/daos/MessageDao.kt and app/src/main/java/com/vjaykrsna/nanoai/core/data/repositories/MessageRepository.kt (depends on T017)
- [ ] T019 [US1] Ensure ChatFeatureCoordinator and ChatUseCases in app/src/main/java/com/vjaykrsna/nanoai/feature/chat/domain/ use encrypted repository APIs and propagate errors (depends on T018)
- [ ] T020 [US1] Update ChatViewModel and PersonaSwitcherViewModel in app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/ to show offline indicator and handle encrypted message failures gracefully (depends on T019)

## Phase 4: User Story 2 - Model Library Management (Priority: P1)

### Tests
- [ ] T021 [P] [US2] Extend ModelLibraryViewModel tests for download queue/error surfacing in app/src/test/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryViewModelTest.kt
- [ ] T022 [P] [US2] Add instrumentation test for pause/resume/delete flows under connectivity changes in app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/ui/ModelLibraryFlowTest.kt

### Implementation
- [ ] T023 [US2] Harden DownloadTask handling and concurrency defaults in app/src/main/java/com/vjaykrsna/nanoai/core/data/download/ and related use cases app/src/main/java/com/vjaykrsna/nanoai/feature/library/domain/ (depends on T021)
- [ ] T024 [US2] Improve model deletion safety notifications and inference stop hooks in app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryViewModel.kt and UI components app/src/main/java/com/vjaykrsna/nanoai/feature/library/ui/ (depends on T023)
- [ ] T025 [US2] Enforce checksum/signature verification for downloads (FR-041) with tests in app/src/test/java/com/vjaykrsna/nanoai/core/data/download/DownloadVerifierTest.kt and instrumentation coverage in app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/ui/ModelDownloadIntegrityTest.kt (depends on T023)
- [ ] T026 [US2] Remove nested sub-ViewModels inside ModelLibraryViewModel while keeping UseCase/Repository flow; consolidate state holders in app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryViewModel.kt and update corresponding tests (depends on T021)

## Phase 5: User Story 3 - Personas & Context Control (Priority: P2)

### Tests
- [ ] T027 [P] [US3] Add persona switch test covering continue vs new thread choice in app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/ui/PersonaSwitchTest.kt

### Implementation
- [ ] T028 [US3] Simplify persona switching flow in PersonaSwitcherViewModel and related use cases in app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/PersonaSwitcherViewModel.kt and app/src/main/java/com/vjaykrsna/nanoai/feature/chat/domain/PersonaUseCases.kt to reduce redundant indirection while keeping architecture contract (depends on T008)
- [ ] T029 [US3] Persist persona switch choice per settings in app/src/main/java/com/vjaykrsna/nanoai/core/data/datastore/ and surface in chat UI (depends on T028)
- [ ] T030 [US3] Consolidate fragmented chat UI state classes in app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/state/ to reduce duplication while preserving sealed-state patterns; update composables and tests accordingly (depends on T028)

## Phase 6: User Story 4 - Settings, Privacy & Backup (Priority: P2)

### Tests
- [ ] T031 [P] [US4] Add export/import round-trip test ensuring warning on unencrypted bundle in app/src/test/java/com/vjaykrsna/nanoai/feature/settings/export/ExportImportTest.kt

### Implementation
- [ ] T032 [US4] Ensure first-launch disclaimer logging and privacy dashboard wiring in app/src/main/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsViewModel.kt and UI in app/src/main/java/com/vjaykrsna/nanoai/feature/settings/ui/ (depends on T031)
- [ ] T033 [US4] Harden API credential encryption paths in app/src/main/java/com/vjaykrsna/nanoai/core/security/EncryptedSecretStore.kt and related DataStore schemas app/src/main/java/com/vjaykrsna/nanoai/core/data/datastore/ (depends on T005)
- [ ] T034 [US4] Enforce export/import warnings for unencrypted bundles across settings and chat entry points with tests in app/src/test/java/com/vjaykrsna/nanoai/feature/settings/export/ExportWarningTest.kt and instrumentation coverage in app/src/androidTest/java/com/vjaykrsna/nanoai/feature/settings/ui/ExportFlowTest.kt (depends on T031)

## Phase 7: User Story 5 - Robust Shell & Navigation (Priority: P3)

### Tests
- [ ] T035 [P] [US5] Expand shell navigation tests for connectivity banners and progress center in app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ShellNavigationTest.kt

### Implementation
- [ ] T036 [US5] Refine shell state handling for connectivity and queued jobs in app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModel.kt and UI components app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/ (depends on T035)
- [ ] T037 [US5] Add connectivity-aware retry/backoff logic tests for shell progress center and queue resume behavior in app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ShellRetryTest.kt (depends on T035)

## Phase 8: Polish & Cross-Cutting

- [ ] T038 [P] Update docs/architecture/ARCHITECTURE.md and specs/001-foundation/spec.md to reflect encryption, simplified indirections, and model library changes (depends on T017, T023, T028)
- [ ] T039 [P] Refresh quickstart validation checklist in specs/001-foundation/quickstart.md with offline encryption checks and model deletion cases (depends on T038)
- [ ] T040 Run gates: ./gradlew spotlessCheck detekt testDebugUnitTest verifyCoverageThresholds and capture summaries in specs/001-foundation/research.md (depends on T036)
- [ ] T041 [P] Run coverage report, map gaps by layer (ViewModel/Domain, Data, UI), and add targeted test backfill tasks to meet ≥75/70/65 thresholds; record in specs/001-foundation/research.md
- [ ] T042 [P] Replace manual fakes with MockK where applicable (prioritize chat/library tests) in app/src/test/java/... and document reductions in specs/001-foundation/research.md
- [ ] T043 [P] Add accessibility audit and fixes (semantics, focus order, contrast, touch targets) for chat, model library, and shell screens; cover with UI tests in app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/AccessibilityTest.kt
- [ ] T044 [P] Add performance validation via macrobenchmarks for startup and navigation budgets (<1.5s cold start, <5% jank) using macrobenchmark module; document results in specs/001-foundation/research.md
- [ ] T045 [P] Validate provider gateway behaviors (config validation, status messaging, unsupported capability errors) with tests in app/src/test/java/com/vjaykrsna/nanoai/core/network/ProviderGatewayTest.kt and app/src/androidTest/java/com/vjaykrsna/nanoai/feature/settings/ui/ProviderStatusTest.kt

---

## Phase Dependencies
- Setup → Foundational → User Stories (US1–US5 in priority order) → Polish
- Foundational tasks T004–T010 block all story phases; US phases can proceed in parallel after Foundational completes, respecting per-task dependencies.

## Parallel Opportunities
- Tasks marked [P] can be executed in parallel where file paths do not overlap (e.g., T001–T003, T007–T008, T011–T012, T017–T018).
