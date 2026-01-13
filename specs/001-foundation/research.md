# Phase 0 Research: Offline Multimodal nanoAI Assistant

## Phase 1 Setup (T001–T003)

### T001: Prerequisite snapshot
- FEATURE_DIR: /home/vijay/Personal/myGithub/nanoAI/specs/001-foundation
- AVAILABLE_DOCS: research.md, data-model.md, contracts/, quickstart.md, tasks.md
- Source: .specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks

### T002: Scope alignment with guardrails
- AGENTS.md alignment: Kotlin-only, Clean Architecture flows (Composable → ViewModel → UseCase → Repository → DataSource), offline-first with encrypted secrets/sensitive data, performance budgets (cold start <1.5s, jank <5%), coverage floors (VM ≥75%, UI ≥65%, Data ≥70%), and no direct ViewModel → Repository shortcuts.
- Architecture doc alignment: matches Clean Architecture layers, ViewModelStateHost pattern, feature-owned domain/data slices, and NanoAIResult error model. No conflicts identified; proceed with encryption/storage hardening and offline UX emphasized in spec.

### T003: Coverage gates and CI commands
- Coverage thresholds: UI 65%, VIEW_MODEL 75%, DATA 70% from config/testing/coverage/coverage-metadata.json; layer mapping in layer-map.json drives classifications; verifyCoverageThresholds wired via coverage-thresholds.gradle.kts.
- Quality gates (blocking unless noted): spotlessCheck (formatting), detekt + detektMain + detektTest (static analysis), :app:verifyCoverageThresholds (layered coverage), :app:roboScreenshotDebug (screenshot diffs), ciManagedDeviceDebugAndroidTest (warning gate), :macrobenchmark:verifyMacrobenchmarkPerformance (startup/navigation/jank budgets).
- CI run expectations: ./gradlew spotlessCheck detekt detektMain detektTest :app:verifyCoverageThresholds :app:roboScreenshotDebug ciManagedDeviceDebugAndroidTest :macrobenchmark:verifyMacrobenchmarkPerformance.

## Mobile LLM Runtime Options

### MediaPipe Generative (LiteRT)
- **Decision**: Use MediaPipe Generative (LiteRT) for initial on-device inference.
- **Rationale**: Optimized for Android with delegate support; aligns with offline, small-model goals.
- **Notes**: Keep a thin abstraction so runtime can evolve without touching UI.

### TensorFlow Lite / MLC LLM / ONNX Runtime / ExecuTorch
- **Decision**: Keep as evaluated options for future releases.
- **Rationale**: Provide flexibility for different model formats and ecosystems.
- **Action**: Design runtime interfaces to be backend-agnostic.

## Offline Data & Model Management
- Use Room for chat, messages, models, personas.
- Download manager: pause/resume, checksum verification, concurrency limit (default 1).
- Only warn about storage where necessary; avoid blocking flows without reason.

## Cloud API Integrations
- Support OpenAI-compatible and Gemini endpoints via Retrofit + Kotlin Serialization.
- No dedicated mobile SDK coupling; talk directly to HTTP APIs.
- Allow user-defined endpoints (OpenAI-compatible) with encrypted API keys.

## UX & Accessibility
- Compose Material 3 with sidebar shell and command palette.
- Adaptive layouts for phones/tablets.
- Accessibility: TalkBack labels, dynamic type, contrast.
- Offline-first UX: banners, queued actions, clear distinction between local/cloud responses.

## Performance & Observability
- Budgets: cold start <1.5s, local response ~2s median, low jank.
- Macrobenchmarks for startup and key flows.
- Local-only logs; provide optional export for debugging.

## Code Quality & Security
- Formatting: Spotless with Kotlin style; Detekt for static analysis (see `config/quality`).
- Secrets: store via encrypted mechanisms; no plaintext secrets in repo or logs.
- Downloads: verify integrity where manifests/checksums exist.

## Test Coverage & CI
- Coverage targets: ViewModel ≥75%, UI ≥65%, Data ≥70%.
- CI: `./gradlew spotlessCheck detekt testDebugUnitTest verifyCoverageThresholds`.
- Contract tests validate OpenAPI and schemas in this directory.

These findings shape the implementation details in `plan.md` and the entities in `data-model.md`.
