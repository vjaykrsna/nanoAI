# Implementation Plan: UI/UX — Polished Product-Grade Experience

**Branch**: `003-UI-UX` | **Date**: 2025-10-06 | **Spec**: [/home/vijay/Personal/myGithub/nanoAI/specs/003-UI-UX/spec.md](/home/vijay/Personal/myGithub/nanoAI/specs/003-UI-UX/spec.md)
**Input**: Feature specification from `/home/vijay/Personal/myGithub/nanoAI/specs/003-UI-UX/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from file system structure or context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Fill the Constitution Check section based on the content of the constitution document.
4. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, agent-specific template file (e.g., `.github/copilot-instructions.md` for GitHub Copilot).
7. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Align the existing nanoAI Android app with the polished multi-modal UX outlined in the spec and refreshed overview.md. The plan focuses on re-architecting the home hub, navigation shell, and mode surfaces (Chat, Image, Audio, Code, Translate) so they share a coherent Material 3 design system, responsive layouts, and consistent async feedback while preserving current data flows and inference orchestration.

## Technical Context
**Language/Version**: Kotlin 1.9.x (JDK 17 target)  
**Primary Dependencies**: Jetpack Compose Material 3, AndroidX Navigation Compose, Hilt, Kotlin Coroutines/Flow, Room, DataStore, WorkManager, Retrofit + Kotlin Serialization, Coil, MediaPipe LiteRT  
**Storage**: Room (SQLite) for structured data, Encrypted DataStore for preferences, on-device file storage for models/assets  
**Testing**: JUnit5 + kotlinx-coroutines-test + Turbine, Mockito/kotlinx testing doubles, Compose UI tests (ComposeTestRule + Espresso interop), Macrobenchmark harness
**Target Platform**: Android 12L+ (minSdk 31) with compile/targetSdk 36, optimized for phones, tablets, and foldables  
**Project Type**: Mobile (Android app module with supporting macrobenchmark project)  
**Performance Goals**: Perceived UI interactions ≤100 ms, content refresh ≤500 ms, FMP ≤300 ms on Pixel 7 class, cold start <1.5 s, frame drops <5%  
**Constraints**: Offline-first UX, Material 3 compliance, light/dark/auto themes, command palette open in <50 ms, base APK <100 MB (dynamic model modules), accessibility coverage (TalkBack/keyboard)  
**Scale/Scope**: Multi-surface shell covering 5 primary modes + History + Library + Settings; ~12 Compose screens with shared components and responsive variants

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Kotlin-First Clean Architecture**: UI updates stay in Compose layers (`feature/uiux`, `ui/components`) with state lifted into existing ViewModels; any new orchestration lives in Kotlin use cases or repositories to preserve Kotlin-only boundaries.
- **Polished Material UX**: Apply Material 3 navigation patterns (Modal/Permanent drawers, top bars) per Jetpack Compose guidance on `ModalNavigationDrawer`/`ModalDrawerSheet`; enforce accessibility semantics, dynamic color, and sub-100 ms touch feedback.
- **Resilient Performance & Offline Readiness**: Maintain offline caches (Room/DataStore), queue heavy work via WorkManager, surface connectivity banners without blocking interactions, and reuse progress center for queued jobs.
- **Automated Quality Gates**: Extend Compose UI + Macrobenchmark tests for home hub transitions, add ViewModel unit tests for shell state, ensure Detekt/Spotless/CI pipelines remain green.
- **Privacy & Data Stewardship**: No new sensitive data; respect existing consent toggles in `UiPreferencesStore`, gate telemetry, and provide inline context for exports/history actions.
- **AI Inference Integrity**: Shell refactor preserves `InferenceOrchestrator` flows, reusing integrity checks (SHA-256) and fallback logic (local → cloud) without bypassing validation, while exposing model controls through right sidebar.
- **Up-to-Date Documentation and Best Practices**: Consulted Jetpack Compose Material 3 navigation drawer documentation via Context7 (`ModalNavigationDrawer`, `ModalDrawerSheet`) to confirm latest adaptive navigation patterns; additional findings captured in research.md.
- **Streamlined and Clean Codebase**: Consolidate scattered scaffolds/drawers into `feature/uiux` shell, eliminating redundant legacy UI fragments and keeping only modern Compose implementations.

## Project Structure

### Documentation (this feature)
```
/home/vijay/Personal/myGithub/nanoAI/specs/003-UI-UX/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md            # generated by /tasks
```

### Source Code (repository root)
```
/home/vijay/Personal/myGithub/nanoAI/
├── app/
│   ├── src/main/java/com/vjaykrsna/nanoai/
│   │   ├── feature/
│   │   │   ├── chat/
│   │   │   ├── library/
│   │   │   ├── settings/
│   │   │   ├── sidebar/
│   │   │   └── uiux/            # central shell + home hub refactor target
│   │   ├── ui/
│   │   │   ├── components/
│   │   │   ├── navigation/
│   │   │   ├── sidebar/
│   │   │   └── theme/
│   │   ├── core/
│   │   │   ├── data/
│   │   │   ├── designsystem/
│   │   │   └── domain/
│   │   └── inference/
│   ├── src/main/res/
│   ├── src/androidTest/
│   └── src/test/ (includes contract specs)
├── macrobenchmark/
│   └── src/main/java/com/vjaykrsna/nanoai/macrobenchmark/
└── docs/ (architecture references)
```

**Structure Decision**: Mobile application — Android app module with shared Compose UI shell and supporting macrobenchmark module.

## Phase 0: Outline & Research
1. Validate Material 3 navigation patterns for multi-pane shells (Modal vs Permanent drawers, adaptive layouts) using current Compose recommendations and Context7 references.
2. Audit existing app surfaces to map gaps against overview.md (command palette, progress center, offline banners, right-sidebar controls) and prioritize remediation.
3. Confirm performance + accessibility tooling (macrobenchmarks, Compose semantics, TalkBack instrumentation) needed to validate 100 ms interactions and offline affordances.
4. Capture findings with Decision/Rationale/Alternatives formatting in `/home/vijay/Personal/myGithub/nanoAI/specs/003-UI-UX/research.md`.

**Output**: research.md with clarified shell strategy, responsive breakpoints, async feedback plan, and validation tooling approach.

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. Document UI state models in `data-model.md`: home hub grid, navigation shell state (left/right sidebars, command palette), global feedback systems (toasts, progress center), offline banners, and persisted preferences.
2. Define interaction contracts under `/home/vijay/Personal/myGithub/nanoAI/specs/003-UI-UX/contracts/` describing ViewModel intents/state reducers for shell actions (openMode, toggleSidebar, queueGeneration, resumeOfflineJob, etc.) and Compose UI test scaffolds.
3. Outline failing tests (Compose + ViewModel) tied to each contract to drive TDD, covering streaming responses, undo flows, offline retries, and theme toggles.
4. Produce `quickstart.md` with environment prerequisites, commands for running unit/UI/macrobenchmark tests, and manual verification checklist for navigation, command palette, progress center, and offline recovery.
5. Run `.specify/scripts/bash/update-agent-context.sh copilot` to record new components and shared patterns after artifacts are generated, keeping the agent file under 150 lines.

**Output**: data-model.md, contracts/* definitions, quickstart.md, and refreshed Copilot agent context ready for /tasks.

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Seed from Phase 1 artifacts: each contract intent → ViewModel unit test + Compose UI test pair, each new state model → persistence + UI binding task, each accessibility/performance guarantee → audit task.
- Prioritize shell refactor (navigation scaffold, command palette), then per-mode surface alignment (Chat/Image/Audio/Code/Translate), followed by global systems (progress center, settings) and polish (animations, offline banners).
- Capture data migration steps (Room/DataStore defaults) as explicit tasks with verification.

**Ordering Strategy**:
- Start with shared shell state models/tests, then implement navigation + command palette, proceed to mode-specific UI, and finish with performance/accessibility hardening.
- Annotate independent mode-surface updates as `[P]` for parallel execution once shell foundation lands.

**Estimated Output**: 28-32 ordered tasks with `[P]` markers enabling parallel surface work.

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
| — | — | — |

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [ ] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented

---
*Based on Constitution v1.3.0 - See `.specify/memory/constitution.md`
