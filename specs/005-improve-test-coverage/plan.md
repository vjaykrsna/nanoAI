
# Implementation Plan: Improve Test Coverage for nanoAI

**Branch**: `005-improve-test-coverage` | **Date**: 2025-10-10 | **Spec**: [/specs/005-improve-test-coverage/spec.md](spec.md)
**Input**: Feature specification from `/specs/005-improve-test-coverage/spec.md`

## Summary
Elevate confidence in nanoAI releases by closing automated test gaps across ViewModel, Compose UI, and data layers, delivering reliable coverage dashboards that highlight progress toward the newly ratified thresholds (ViewModel 75%, UI 65%, Data 70%) and ensuring stakeholders can approve releases with clear evidence of quality safeguards.

## Technical Context
**Language/Version**: Kotlin 1.9.x (AGP per repo baseline)  
**Primary Dependencies**: Jetpack Compose Material 3, AndroidX Lifecycle ViewModel, Kotlin Coroutines, Room, Retrofit, WorkManager, JaCoCo (to be added)  
**Storage**: Room database, DataStore preferences for lightweight configuration  
**Testing**: JUnit6 (Gradle), AndroidX Compose UI testing, Mockito/Kotlinx Coroutines Test, Robolectric where viable
**Target Platform**: Android 8.0+ (minSdk per project) with instrumentation across Pixel reference devices  
**Project Type**: Mobile (Android app + supporting scripts)  
**Performance Goals**: Maintain UX responsiveness (<100ms feedback, <500ms content update) while executing tests within CI budget (<20 min total), coverage thresholds ≥ ViewModel 75% / UI 65% / Data 70%  
**Constraints**: Offline-friendly assertions must remain valid; CI must remain green with JaCoCo overhead <10% runtime; adhere to Detekt/spotless gates
**Migration Context**: Include JUnit5 to JUnit6 migration (drop legacy junit alias, adopt Jupiter extensions, update test annotations) to modernize test framework while ensuring deterministic coroutines and accessibility assertions.
**Scale/Scope**: Focus on high-impact flows (Conversation, Message composition, History) spanning ~8 ViewModels, core Repository layer, and Compose surfaces used daily

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Kotlin-First Clean Architecture**: Test additions target ViewModels, repositories, and Compose UI without introducing non-Kotlin code; any new helpers remain within existing modular boundaries and leverage dependency injection to keep layers isolated.
- **Polished Material UX**: Compose UI tests will verify Material 3 interactions, TalkBack semantics, and responsiveness to dynamic type to preserve tactile feedback budgets (<100ms) and ensure progress indicators appear for longer tasks.
- **Resilient Performance & Offline Readiness**: Data-layer tests will simulate offline scenarios (Room cache, Retrofit error fallbacks) to guarantee graceful degradation while respecting cold-start and frame stability constraints documented in the constitution.
- **Automated Quality Gates**: The plan introduces JaCoCo-backed coverage reporting tied to CI (unit + instrumentation) and extends test suites to satisfy coverage thresholds, ensuring gate enforcement before merges.
- **Privacy & Data Stewardship**: New telemetry around coverage avoids user PII, and test data will rely on synthetic fixtures respecting encryption and consent pathways already in place.
- **AI Inference Integrity**: Coverage will include AI-related ViewModels and repositories, validating local/remote fallbacks with deterministic mocks to uphold integrity checks.
- **Up-to-Date Documentation and Best Practices**: Research phase references Context7 MCP guidance for JaCoCo integration with Kotlin Multiplatform/Compose projects and keeps documentation refreshed in quickstart.md.
- **Streamlined and Clean Codebase**: Deprecated or redundant testing utilities will be pruned in favor of centralized helpers, preventing legacy cruft while scaling new suites.

## Project Structure

### Documentation (this feature)
```
specs/005-improve-test-coverage/
├── plan.md      
├── research.md  
├── data-model.md
├── quickstart.md
├── contracts/   
└── tasks.md     
```

### Source Code (repository root)
```
android/
└── app/src/
      ├── main/java/com/vjaykrsna/nanoai/
      │   ├── core/          # domain, repositories, data sources
      │   ├── feature/chat/  # primary ViewModels & UI to be covered
      │   └── feature/history/
      ├── main/res/
      ├── androidTest/java/com/vjaykrsna/nanoai/
      │   ├── compose/       # Compose UI instrumentation tests (to expand)
      │   └── data/          # DAO instrumentation tests
      └── test/java/com/vjaykrsna/nanoai/
            ├── viewmodel/     # JVM unit tests for ViewModels
            └── data/          # Repository/unit data-layer tests

scripts/
└── coverage/              # (new) helpers for JaCoCo report aggregation
```

**Structure Decision**: Mobile-first Android app; work centers on `app/src/main/java` for production logic with parallel `test/` and `androidTest/` trees plus new coverage scripts.

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - Validate JaCoCo configuration compatibility with Kotlin + Compose + Hilt modules.
   - Confirm recommended tooling for Compose UI screenshot/semantics verification to keep tests deterministic.
   - Determine CI resource impact and optimal report formats (HTML/XML) for stakeholder consumption.

2. **Generate and dispatch research agents**:
   ```
   Research JaCoCo Gradle integration for Kotlin Multiplatform + Android modules.
   Research Compose UI testing patterns for accessibility and semantics assertions.
   Research Room DAO instrumentation strategies for offline-first validation.
   Benchmark CI coverage report presentation (HTML vs XML vs badge dashboards).
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: final approach (e.g., JaCoCo Gradle plugin with merged report task)
   - Rationale: ties to constitution (automation, accessibility)
   - Alternatives considered: c8, Firebase Test Lab metrics, other coverage libraries

**Output**: `research.md` with all targeted unknowns resolved and cross-linked to spec clarifications.

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Document `CoverageSummary` (layer metrics, thresholds, trend history), `TestSuiteCatalogEntry` (owner, scope, coverage delta), and `RiskRegisterItem` (gap severity, mitigation ETA).
   - Capture relationships between entities (catalog entries roll up into coverage summaries; risk items reference suites).

2. **Generate quality contracts** from functional requirements:
   - Define reporting contract describing coverage aggregation output (JSON schema) and update quickstart with CLI/Gradle commands to regenerate reports.
   - Specify acceptance contract for CI gating (threshold enforcement) stored in `/contracts/coverage-report.schema.json`.

3. **Plan contract tests** from contracts:
   - Outline JVM unit tests that validate report parsing and threshold enforcement; instrumentation tests check UI coverage dashboards.
   - Mark them intentionally failing to drive TDD once implementations land.

4. **Extract test scenarios** from user stories:
   - Quickstart enumerates verifying coverage summary after CI run, adding new risk scenario, handling offline device farm.

5. **Update agent file incrementally** (O(1) operation):
   - Run `.specify/scripts/bash/update-agent-context.sh copilot` to register JaCoCo + coverage context for future automation hints.
   - Keep manual guidelines intact while logging new feature additions in the last-changes section.

**Output**: `data-model.md`, `/contracts/coverage-report.schema.json`, quickstart checklist, and updated agent context ready for `/tasks`.

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Each contract → contract test task [P]
- Each entity → model creation task [P] 
- Each user story → integration test task
- Implementation tasks to make tests pass

**Ordering Strategy**:
- TDD order: Tests before implementation 
- Dependency order: Models before services before UI
- Mark [P] for parallel execution (independent files)

**Estimated Output**: 25-30 numbered, ordered tasks in tasks.md

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [x] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [ ] Complexity deviations documented

---
*Based on Constitution v1.4.0 - See `.specify/memory/constitution.md`*
