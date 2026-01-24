# Implementation Plan: Offline Multimodal nanoAI Assistant

**Branch**: `001-foundation`  
**Date**: 2025-09-30  
**Spec**: `specs/001-foundation/spec.md`

## Summary
Deliver a polished, offline-first Android multimodal AI assistant with strong UX, clean architecture, and strict quality gates. The app provides offline chat via MediaPipe Generative (LiteRT), optional cloud fallback through configured providers, a model library with download controls, personas, and export/import flows. All implementation follows Kotlin-first clean architecture and the rules in `AGENTS.md`.

## Technical Context
- Language: Kotlin 2.2.x, JDK 17
- UI: Jetpack Compose Material 3
- DI: Hilt
- Async: Kotlin Coroutines
- Storage: Room (SQLite) for chats/models/personas; encrypted preferences/DataStore for credentials and privacy
- Networking: Retrofit + Kotlin Serialization, OkHttp
- Runtime: MediaPipe Generative (LiteRT) for local models
- Testing: JUnit5, Turbine, Robolectric, Compose UI tests, Macrobenchmark
- Performance: Cold start <1.5s, dropped frames <5%, queue flush <500ms
- Constraints: Offline-first, no plaintext secrets, explicit warnings for unencrypted exports

## Project Structure

### Specs

Located under `specs/[###-feature]/`:
- `plan.md` — implementation plan
- `research.md` — design decisions and references
- `data-model.md` — entities and relationships
- `quickstart.md` — validation checklist
- `contracts/` — API and schema contracts

### Source Code

Root application module:
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/` — coverage dashboard, model catalog, related assets
- `app/src/main/java/com/vjaykrsna/nanoai/`
  - `core/` — common, coverage, data, device, di, domain, maintenance, model, network, runtime, security, telemetry
  - `feature/` — `audio`, `chat`, `image`, `library`, `settings`, `uiux`
  - `shared/` — shared UI and testing utilities
- Tests
  - `app/src/test/java/com/vjaykrsna/nanoai/` — JVM unit tests
  - `app/src/androidTest/java/com/vjaykrsna/nanoai/` — instrumentation & UI tests
- Performance
  - `macrobenchmark/` — startup and UI benchmarks

## Phase 0: Research & Alignment
- Confirm library choices and patterns from official docs.
- Align with `docs/architecture/ARCHITECTURE.md` and `AGENTS.md` for clean architecture and coverage targets.
- Capture key decisions in `research.md` (Decision, Rationale, Alternatives).

## Phase 1: Architecture & Data Model
- Finalize entities and relationships in `data-model.md` to match actual Room/DataStore models.
- Ensure separation:
  - UI → ViewModel → UseCase → Repository → DataSource
  - No direct cross-layer shortcuts.
- Design model library, personas, privacy, and coverage entities to support offline-first behavior.

## Phase 2: Implementation
- Implement core domains and repositories for chat, models, personas, and settings.
- Implement local runtime abstraction for MediaPipe Generative.
- Wire cloud providers behind a unified gateway with encrypted credentials.
- Build Home Hub, Chat, Model Library, and Settings screens using shared UI components.
- Ensure all sensitive data uses `EncryptedSecretStore` or equivalent.

## Phase 3: UX, Offline, and Error Handling
- Implement command palette, sidebars, and progress center.
- Ensure offline flows: queued jobs, connectivity banners, graceful degradation.
- Standardize errors using existing result and error types.
- Add first-launch disclaimer and privacy dashboard, with clear messaging and logging of consent.

## Phase 4: Tests & Quality Gates
- Add/extend unit tests for:
  - Use cases and repositories
  - Runtime and download orchestration
  - Personas and export/import
- Add Compose/UI tests for:
  - Shell navigation, chat, model downloads, settings
- Ensure:
  - ViewModel ≥75% coverage
  - UI ≥65% coverage
  - Data ≥70% coverage
- Use `./gradlew spotlessCheck detekt testDebugUnitTest verifyCoverageThresholds` as the main gate.

## Phase 5: Performance & Stabilization
- Use macrobenchmarks to validate cold start and navigation performance.
- Measure local inference response times on reference devices.
- Fix regressions, simplify over-complex code paths, and remove dead code.

## Notes
- Follow `AGENTS.md` for security, performance, and architecture rules.
- Any deviations from this plan must update this spec and relevant docs.

## Phase 2 Remediation Plan (T006)
- Approach: adopt column-level encryption for chat messages now while preparing a follow-on SQLCipher path for full-database encryption; use AES/GCM with a symmetric key sealed via `EncryptedSecretStore`.
- Schema deltas (messages table): add `ciphertext` (BLOB/Base64, non-null), `iv` (12-byte IV/Base64, non-null), `encryption_version` (INT, default 1), and `search_text` (TEXT, redacted preview for search/UI). Drop or null out the legacy `text` column after migration; DAO/domain should only surface decrypted text.
- Migration steps:
  1. Add the new columns with defaults; keep plaintext `text` temporarily for backfill.
  2. Introduce `MessageCrypto` + `MessageKeyProvider` that pulls an AES-256 key from `EncryptedSecretStore` (alias `nanoai.message`), encrypts existing `text` with per-row random IVs, and writes `ciphertext`/`iv`/`encryption_version` + redacted `search_text`.
  3. After backfill, null the `text` column and update DAOs/repositories to read/write encrypted fields only.
  4. Add migration/DAO tests (T009/T016) expecting ciphertext persistence and decrypt-on-read behavior; ensure failure surfaces error to repository/ViewModel layers.
- Key management & rotation: store one AES key in `EncryptedSecretStore`, version tracked via `encryption_version`; future rotations can re-encrypt rows with a new key/version and preserve backward decryption until all rows are upgraded.
- Validation: run migrations on sample DBs, verify offline flows still work, and update `ARCHITECTURE.md`/contracts after the schema change (T010).

## Architecture Guard Checklist (T012)
- [ ] ViewModels depend on consolidated UseCases only; no direct Repository injections or helper facades that bypass UseCases.
- [ ] New consolidated facades (PersonaUseCases, PreferencesUseCases, ModelCatalogUseCase) expose typed operations without leaking repository implementations to the presentation layer.
- [ ] Code reviews must reject PRs that introduce ViewModel → Repository shortcuts or bypass encryption in the data layer.
- [ ] Add/keep unit tests for ViewModels to verify they call UseCase seams (mock UseCases, never repositories) and integration tests to cover encrypted chat persistence.
