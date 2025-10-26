# Tasks: Comprehensive Architecture Layer Violations Cleanup

**Input**: Design documents from `/specs/001-foundation/`, architectural issues from `/findings.md`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Task List

### Phase 1: Setup
- [x] T001 Audit current architectural violations in `findings.md` and verify issues exist in codebase
- [x] T002 Create backup branch for architectural changes with format `arch-cleanup-{timestamp}`

### Phase 2: Foundational
- [x] T003 [P] Standardize result types across domain layer - migrate all UseCases to `NanoAIResult<T>` in `app/src/main/java/com/vjaykrsna/nanoai/core/common/NanoAIResult.kt`
- [x] T004 [P] Create base repository interface contracts in `app/src/main/java/com/vjaykrsna/nanoai/core/data/repository/` for proper abstraction
- [x] T005 [P] Add injected CoroutineDispatcher to repository constructors following pattern in `app/src/main/java/com/vjaykrsna/nanoai/core/common/IoDispatcher.kt`

### Phase 3: UseCase Layer Cleanup [US1]
**Story Goal**: Eliminate single responsibility violations in UseCases by splitting monolithic implementations
**Independent Test Criteria**: Each UseCase handles exactly one business operation with clear boundaries
**Tests**: Unit tests verify single responsibility and proper result type usage

- [x] T006 Split `SendPromptAndPersonaUseCase` into focused UseCases (`SendPromptUseCase`, `SwitchPersonaUseCase`) in `app/src/main/java/com/vjaykrsna/nanoai/feature/chat/domain/`
- [x] T007 Split `ModelDownloadsAndExportUseCase` into focused UseCases for downloads, verification, and export operations in `app/src/main/java/com/vjaykrsna/nanoai/feature/library/domain/`
- [x] T008 Fix `SettingsOperationsUseCase` - remove custom CoroutineScope and inject dispatcher instead in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/domain/SettingsOperationsUseCase.kt`
- [x] T009 Create `ModelCatalogUseCase` for ModelLibraryViewModel operations (getAllModels, recordOfflineFallback, getModel, upsertModel) in `app/src/main/java/com/vjaykrsna/nanoai/feature/library/domain/`
- [x] T010 Create `HuggingFaceCatalogUseCase` for HuggingFaceLibraryViewModel listModels operation in `app/src/main/java/com/vjaykrsna/nanoai/feature/library/domain/`
- [x] T011 Create `ApiProviderConfigUseCase` for SettingsViewModel CRUD operations in `app/src/main/java/com/vjaykrsna/nanoai/feature/settings/domain/`
- [x] T012 Create `ImageGalleryUseCase` for image gallery operations in `app/src/main/java/com/vjaykrsna/nanoai/feature/image/domain/`
- [x] T013 Create `ConversationUseCase` for ChatViewModel conversation operations in `app/src/main/java/com/vjaykrsna/nanoai/feature/chat/domain/`
- [x] T014 Update all UseCase result types to use `NanoAIResult<T>` consistently
- [x] T015 Update UseCase unit tests to verify new split responsibilities and result types

### Phase 4: Repository Layer Cleanup [US2]
**Story Goal**: Fix repository violations by moving business logic to UseCases and implementing proper abstractions
**Independent Test Criteria**: Repositories contain only data access logic with injected dispatchers
**Tests**: Unit tests verify repository interfaces and data operations

- [x] T016 Split `ShellStateRepository` (343 lines) into focused repositories: `NavigationRepository`, `ConnectivityRepository`, `ThemeRepository`, `ProgressRepository` in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/data/`
- [x] T017 Move business logic from `ModelManifestRepositoryImpl` (complex manifest validation, resolution strategies, caching, telemetry) to appropriate UseCases
- [x] T018 Remove custom CoroutineScope creation from repositories and use injected dispatchers
- [x] T019 Implement consistent offline error handling across all network repositories (HuggingFaceCatalogRepositoryImpl, ModelManifestRepositoryImpl)
- [x] T020 Create repository interface contracts for all repository implementations where missing
- [x] T021 Add UseCase interfaces for consistency (currently only ModelDownloadsAndExportUseCase has an interface)
- [x] T022 Update repository unit tests for new interfaces and injected dispatchers

### Phase 5: UI Layer Cleanup [US3]
**Story Goal**: Break down monolithic UI components that violate single responsibility principle
**Independent Test Criteria**: No UI files exceed 400 lines, each component has single clear responsibility
**Tests**: UI tests verify component behavior after refactoring
./
- [x] T023 Refactor `NanoShellScaffold.kt` (750 lines) into smaller focused composables by feature/responsibility boundaries
- [x] T024 Refactor `ShellViewModel.kt` (433 lines) by splitting into focused ViewModels (NavigationViewModel, ConnectivityViewModel, etc.)
- [x] T025 Update Hilt modules for new ViewModel structure and dependencies
- [x] T026 Update UI tests for refactored components and ViewModels

### Phase 6: ViewModel Integration Updates [US4]
**Story Goal**: Update ViewModels to use new UseCase and repository abstractions
**Independent Test Criteria**: ViewModels only call UseCases, never repositories directly
**Tests**: ViewModel tests verify proper UseCase injection and state management

- [x] T027 Update `ModelLibraryViewModel` to use `ModelCatalogUseCase` instead of calling repository directly
- [x] T028 Update `HuggingFaceLibraryViewModel` to use `HuggingFaceCatalogUseCase` instead of calling repository directly
- [x] T029 Update `SettingsViewModel` to use `ApiProviderConfigUseCase` instead of calling repository directly
- [x] T030 Update `ImageGalleryViewModel` to use `ImageGalleryUseCase` instead of calling repository directly
- [x] T031 Update `ChatViewModel` to use `ConversationUseCase` instead of calling repository directly
- [x] T032 Update `ImageGenerationViewModel` to use `ImageGalleryUseCase` instead of calling repository directly
- [x] T033 Update Hilt dependency injection modules for new UseCases and repository interfaces
- [x] T034 Update ViewModel unit tests to mock new UseCase dependencies
- [x] T035 Update instrumentation tests for ViewModel behavior changes

### Phase 7: Cross-Layer Error Handling [US5]
**Story Goal**: Implement consistent error handling patterns across all architectural layers
**Independent Test Criteria**: All errors propagate through sealed result types with proper user messaging
**Tests**: Integration tests verify error propagation from data to UI layers

- [ ] T036 Implement consistent offline error handling in network repositories
- [ ] T037 Update error telemetry to work with standardized `NanoAIResult<T>` types
- [ ] T038 Add user-friendly error messages for all error scenarios
- [ ] T039 Update error boundaries at architectural layer transitions

### Phase 8: Polish & Validation
**Story Goal**: Final cleanup and validation of architectural changes
**Independent Test Criteria**: All detekt/ktlint checks pass, coverage maintained, no regressions

- [ ] T040 Run full test suite to ensure no regressions from architectural changes
- [ ] T041 Update documentation to reflect new architectural structure
- [ ] T042 Run detekt and ktlint to validate code quality improvements
- [ ] T043 Generate coverage reports to ensure targets maintained
- [ ] T044 Update findings.md with corrected issues and new architectural violations found
- [ ] T045 Create summary of architectural improvements and impact analysis

## Dependencies

- T003 depends on T001 (need to audit current usage)
- T006-T015 depend on T003 (standardized result types needed first)
- T016-T022 depend on T004-T005 (repository interfaces needed first)
- T023-T026 depend on T016 (ShellStateRepository split needed for UI refactoring)
- T027-T035 depend on T006-T022 (new abstractions needed first)
- T036-T039 depend on T003, T014, T022 (consistent error types needed)
- T040-T045 depend on all implementation tasks (T006-T039)


## Implementation Strategy

**MVP Scope**: Complete Phase 3 (UseCase cleanup) as the minimum viable architectural improvement. This addresses the most critical single responsibility violations and missing abstractions.

**Incremental Delivery**: Execute phases in order (3→4→5→6→7→8) to maintain system stability. Each phase delivers independently testable architectural improvements.

**Risk Mitigation**: Create backup branch before starting. Run full test suite after each phase. Maintain coverage targets throughout.

**Success Criteria**:
- All UseCases follow single responsibility principle
- ViewModels only call UseCases, never repositories directly
- Consistent result types (`NanoAIResult<T>`) across domain layer
- Repositories contain only data access logic with injected dispatchers
- No UI files exceed 400 lines
- All detekt blocking rules pass

## Parallel Execution Examples

- Run result type standardization in parallel:
  - `taskctl run --parallel T003 T004 T005`
- Execute UseCase creation in parallel after foundational work:
  - `taskctl run --parallel T009 T010 T011 T012 T013`
- Run repository cleanup in parallel:
  - `taskctl run --parallel T016 T017 T018 T019 T020 T021`
- Execute UI layer refactoring in parallel:
  - `taskctl run --parallel T023 T024`
- Run ViewModel updates in parallel:
  - `taskctl run --parallel T027 T028 T029 T030 T031 T032`
- Run final validation in parallel:
  - `taskctl run --parallel T040 T041 T042 T043 T044 T045`
