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

# Phase 2 Foundational (T004–T014)

## T004: Chat persistence audit
- Message text is stored as a nullable `text` column in [core/data/src/main/java/com/vjaykrsna/nanoai/core/data/db/entities/MessageEntity.kt](core/data/src/main/java/com/vjaykrsna/nanoai/core/data/db/entities/MessageEntity.kt) with no encryption, hashing, or redaction; Room database is the default unencrypted SQLite file.
- [core/data/src/main/java/com/vjaykrsna/nanoai/core/data/db/daos/MessageDao.kt](core/data/src/main/java/com/vjaykrsna/nanoai/core/data/db/daos/MessageDao.kt) reads/writes the `text` column directly and exposes raw values to callers, confirming plaintext storage across insert/query flows.
- Existing DAO tests ([core/data/src/test/java/com/vjaykrsna/nanoai/core/data/db/ChatMessageDaoTest.kt](core/data/src/test/java/com/vjaykrsna/nanoai/core/data/db/ChatMessageDaoTest.kt)) assert plaintext round-trips, reinforcing that current migrations and schema expect unencrypted message bodies.

## T005: Available encryption mechanisms + options
- Secrets path: [core/data/src/main/java/com/vjaykrsna/nanoai/core/security/EncryptedSecretStore.kt](core/data/src/main/java/com/vjaykrsna/nanoai/core/security/EncryptedSecretStore.kt) wraps Android Keystore AES/GCM (256-bit) and stores a single encrypted JSON file. [HuggingFaceCredentialRepository](core/data/src/main/java/com/vjaykrsna/nanoai/core/security/HuggingFaceCredentialRepository.kt) and [ProviderCredentialStore](core/data/src/main/java/com/vjaykrsna/nanoai/core/security/ProviderCredentialStore.kt) rely on it for API tokens; disk content is encrypted, but message bodies never enter this path.
- Room path: [core/data/src/main/java/com/vjaykrsna/nanoai/core/data/db/NanoAIDatabase.kt](core/data/src/main/java/com/vjaykrsna/nanoai/core/data/db/NanoAIDatabase.kt) uses default SQLite; migrations in [NanoAIDatabaseMigrations.kt](core/data/src/main/java/com/vjaykrsna/nanoai/core/data/db/NanoAIDatabaseMigrations.kt) add tables/columns but do not introduce SQLCipher or field-level encryption. Type converters remain plaintext for strings.
- Option A (preferred): full-database encryption via SQLCipher `SupportFactory`, with the passphrase derived from a symmetric key sealed in `EncryptedSecretStore`; migrate by reading plaintext DB, writing to new encrypted DB, then swapping. Pros: encrypts all tables including messages and manifests; Cons: requires key management and migration downtime.
- Option B: column-level encryption for chat messages using an AES/GCM key stored via `EncryptedSecretStore`; add `ciphertext` (BLOB/Base64) column and migrate rows by encrypting `text`, optionally keeping a redacted/FTS-safe `text_search` column for lookups. Pros: narrower blast radius; Cons: still leaves other tables plaintext and complicates queries.
- Option C: hybrid—start with column-level encryption for messages (sensitive) while planning full SQLCipher enablement to cover models/manifests later; avoids blocking on global migration while unblocking chat PII protection.

## T007: Build pipeline profile
- Clean build (no build cache): `./gradlew clean :app:assembleDebug --no-build-cache` in [app/build.gradle.kts](app/build.gradle.kts) path took **2m30s** (86 tasks executed). Dominant work: Kotlin compilation across :core modules and symbol stripping for native libs; configuration cache now stored.
- Incremental build (warm cache/config cache): `./gradlew :app:assembleDebug` finished in **4s** (all tasks up-to-date). Indicates compile avoidance is effective once caches are hot.
- Plugin/config contributors from [build-logic/src/main/kotlin/com/vjaykrsna/nanoai/buildlogic/AndroidApplicationConventionPlugin.kt](build-logic/src/main/kotlin/com/vjaykrsna/nanoai/buildlogic/AndroidApplicationConventionPlugin.kt) and [SharedConfiguration](build-logic/src/main/kotlin/com/vjaykrsna/nanoai/buildlogic/SharedConfiguration.kt): KSP + Hilt + Room codegen, Compose compiler metrics/reports, Spotless/Detekt wired into `check`, and coverage enabled for debug buildTypes. Lint tasks are currently disabled post-evaluation.
- Actions to reach <2m clean build: enable local Gradle build cache (avoid `--no-build-cache`), keep configuration cache warm, run with `-Dorg.gradle.jvmargs` tuned for Kotlin compilation, and consider deferring stripDebugDebugSymbols during local dev. Optional: use Gradle Build Scan to pinpoint slow tasks and evaluate trimming unused managed device configs that inflate configuration time.

## T008: Use-case/repository indirection map
- Inventory: 41 use case/coordinator files across core domain + chat feature; 16 of them are ≤35 lines (pure pass-through to repositories/other use cases). Examples: [GetDefaultPersonaUseCase](core/domain/src/main/java/com/vjaykrsna/nanoai/core/domain/usecase/GetDefaultPersonaUseCase.kt), [ObservePersonasUseCase](core/domain/src/main/java/com/vjaykrsna/nanoai/core/domain/usecase/ObservePersonasUseCase.kt), [ListHuggingFaceModelsUseCase](core/domain/src/main/java/com/vjaykrsna/nanoai/core/domain/library/ListHuggingFaceModelsUseCase.kt), [ModelManifestUseCase](core/domain/src/main/java/com/vjaykrsna/nanoai/core/domain/library/ModelManifestUseCase.kt), [GenerateLeapTextUseCase](core/domain/src/main/java/com/vjaykrsna/nanoai/core/domain/chat/GenerateLeapTextUseCase.kt), and [CallLeapFunctionUseCase](core/domain/src/main/java/com/vjaykrsna/nanoai/core/domain/chat/CallLeapFunctionUseCase.kt).
- Chat feature facade: [ChatFeatureCoordinator](app/src/main/java/com/vjaykrsna/nanoai/feature/chat/domain/ChatFeatureCoordinator.kt) re-exposes six core domain use cases plus persona/model observers without added logic; effectively an extra injection layer atop `ConversationUseCase`/`SendPromptUseCase`/`SwitchPersonaUseCase` and persona/model catalog flows.
- Persona stack redundancy: `GetDefaultPersonaUseCase`, `ObservePersonasUseCase`, and persona-related operations in `ConversationUseCase` overlap; consider consolidating into a `PersonaUseCases` bundle or folding into `ConversationUseCase` to reduce indirection while keeping UI → UseCase → Repository boundaries.
- Model/library thin wrappers: `ListHuggingFaceModelsUseCase`, `HuggingFaceCatalogUseCase`, `ModelManifestUseCase`, and `ListLeapModelsUseCase` mostly forward to repositories; merging into a single `ModelCatalogUseCase` (with typed operations) would cut DI noise and simplify `ChatFeatureCoordinator` wiring.
- Guidance: keep complex orchestrators (`ConversationUseCase`, `SendPromptUseCase`, `DownloadModelUseCase`, `ModelDownloadsAndExportUseCase`) intact; collapse ≤35-line pass-throughs into grouped facades to retain layer purity but remove redundant hop counts in viewmodels.

## T011: Consolidation blueprint for thin use cases
- Sampled 41 use case/coordinator files (200+ total domain/presentation files); 16 are trivial pass-throughs (≤35 LOC) with no domain validation. These add DI/config overhead without improving testability.
- **Keep**: orchestration-heavy flows (`SendPromptUseCase`, `ConversationUseCase`, `ModelDownloadsAndExportUseCase`, `DownloadModelUseCase`, `VerifyDownloadUseCase`, `SwitchPersonaUseCase`, `LocalInferenceUseCase`) because they coordinate multiple repositories/runtime checks.
- **Merge**: persona wrappers (`GetDefaultPersonaUseCase`, `ObservePersonasUseCase`, `ObserveDisclaimerExposureUseCase`, `ObservePrivacyPreferencesUseCase`, `ObserveUiPreferencesUseCase`, `UpdatePrivacyPreferencesUseCase`, `UpdateUiPreferencesUseCase`) into two cohesive facades (`PersonaUseCases`, `PreferencesUseCases`) so ViewModels inject one aggregate instead of 5–7 single-method classes.
- **Merge**: catalog wrappers (`ListHuggingFaceModelsUseCase`, `HuggingFaceCatalogUseCase`, `ModelManifestUseCase`, `ListLeapModelsUseCase`, `GenerateLeapTextUseCase`, `CallLeapFunctionUseCase`) into `ModelCatalogUseCase` with typed operations; retain `ModelDownloadsAndExportUseCase` for download orchestration.
- **Refactor**: replace [ChatFeatureCoordinator](app/src/main/java/com/vjaykrsna/nanoai/feature/chat/domain/ChatFeatureCoordinator.kt) with direct injection of the consolidated use-case bundles into `ChatViewModel`/`PersonaSwitcherViewModel`, preserving ViewModel → UseCase → Repository flow while removing a hop.
- **Outcome targets**: reduce injected constructors per ViewModel by ~3–5 while keeping single-layer boundaries; simplify tests by stubbing aggregated facades instead of multiple no-op wrappers.

## T012: Architecture guard (code review gate)
- Apply the checklist in [specs/001-foundation/plan.md](specs/001-foundation/plan.md) before merging PRs that touch chat/domain: no ViewModel → Repository shortcuts, consolidated facades only, and UseCase seams mocked in tests.
- Require reviewers to verify encrypted persistence paths stay inside data layer abstractions; any ViewModel or UI change that handles plaintext should be rejected unless it flows through the new encryption-aware UseCases.

## T013: Export/import warnings & data classification
- Current UX: Settings export dialog warns “Backups are not encrypted” with a “don’t show again” checkbox; privacy dashboard tracks `exportWarningsDismissed`. `BackupDataSource` logs an `ENCRYPTION_WARNING`, and `ExportServiceImpl.notifyUnencryptedExport` only logs to Logcat.
- Data included: exports bundle personas, provider configs (without API keys, only `hasCredential` flag), and chat threads (titles/persona/model IDs) when `includeChatHistory` is true by default in [SettingsScreenActions](app/src/main/java/com/vjaykrsna/nanoai/feature/settings/ui/SettingsScreenActions.kt#L76-L110). Messages are not exported today but would remain plaintext if added.
- Classification gaps: Sensitive data (personas, chat threads, future encrypted messages) is exported unencrypted with only a dismissible warning; secrets are excluded, but there is no explicit “Secrets vs Sensitive” copy in dialogs or import flows. No chat entry points surface warnings, and imports do not remind users that bundles may contain sensitive content.
- Required warnings/actions: add persistent banner + modal copy distinguishing **Secrets (API keys, credentials)** vs **Sensitive (chat content, personas, providers)**; require explicit confirmation when including chat history; surface post-export toast linking to secure-storage guidance; add import warning banner before executing restore. Tie dismissal to privacy preferences and ensure telemetry logs capture warning exposure for audit.

## T014: Credential encryption/decryption validation
- [core/data/src/test/java/com/vjaykrsna/nanoai/core/security/EncryptedSecretStoreTest.kt](core/data/src/test/java/com/vjaykrsna/nanoai/core/security/EncryptedSecretStoreTest.kt) covers save/get/list/delete round-trips with deterministic clock; verifies metadata/rotation persistence but uses in-memory persistence (Keystore path still unverified on JVM).
- Added [app/src/test/java/com/vjaykrsna/nanoai/core/data/datastore/CredentialStoreTest.kt](app/src/test/java/com/vjaykrsna/nanoai/core/data/datastore/CredentialStoreTest.kt) to assert `ProviderCredentialStore` saves credentials with scoped metadata, reuses existing IDs, resolves values, and no-ops on null deletes.
- Gaps: no instrumentation coverage for AES/GCM `KeystoreSecretCrypto` or end-to-end encrypted file writes; need Robolectric/Android tests that exercise real keystore + `notifyUnencryptedExport` hooks and ensure API keys are only stored via `EncryptedSecretStore`.
