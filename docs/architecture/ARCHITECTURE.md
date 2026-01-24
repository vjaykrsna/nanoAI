# nanoAI Architecture

## Clean Architecture Overview

nanoAI implements **Clean Architecture** with strict separation of concerns across four distinct layers:

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer (Presentation)                                    │
│  ├── Compose Screens & Components                           │
│  ├── ViewModels (UI State Orchestration)                    │
│  └── Navigation & UI State Management                       │
├─────────────────────────────────────────────────────────────┤
│  Domain Layer (Business Logic)                              │
│  ├── UseCases (Single Responsibility Business Operations)   │
│  ├── Business Models & Validation                           │
│  └── NanoAIResult<T> (Consistent Error Handling)            │
├─────────────────────────────────────────────────────────────┤
│  Data Layer (Infrastructure)                                │
│  ├── Repositories (Data Access Abstraction)                 │
│  ├── DAOs & Network Clients                                 │
│  └── External Service Integrations                          │
├─────────────────────────────────────────────────────────────┤
│  Core Layer (Cross-cutting Concerns)                        │
│  ├── Dependency Injection (Hilt)                            │
│  ├── Common Utilities & Extensions                          │
│  └── Telemetry & Error Reporting                            │
└─────────────────────────────────────────────────────────────┘
```

**Key Principles:**
- **Dependency Rule**: Inner layers know nothing about outer layers
- **Single Responsibility**: Each component has one reason to change
- **Dependency Inversion**: High-level modules don't depend on low-level modules
- **Testability**: Each layer can be tested in isolation

## System Architecture

### Application Structure

```
🎯 Application Module (:app)
├── MainActivity - Single activity architecture
├── Feature folders (presentation + domain + data slices that wrap shared core services)
└── Core Infrastructure (cross-cutting services, shared models, DI glue)

⚡ Benchmark Module (:macrobenchmark)
├── Performance validation suite
├── Cold start & frame rate analysis
└── Memory profiling & baseline validation
```

### Feature Organization

Feature folders now own end-to-end slices while still leaning on shared core modules for
primitives (models, gateways, dispatchers):

```
feature/{name}/
├── ui/            - Compose screens & components (presentation)
├── presentation/  - ViewModels & UI state hosts (presentation)
├── domain/        - Feature-specific use cases + coordinators
└── data/          - Repositories + data sources bound to core contracts
```

Each feature keeps the Clean Architecture chain intact: `Composable → ViewModel → UseCase →
Repository → DataSource`. Shared, cross-feature contracts (e.g., connectivity, runtime,
telemetry) remain in `:core` while feature-owned domain/data live beside the UI for tighter
traceability and easier testing.

**Active Feature Folders:** `audio/`, `chat/`, `image/`, `library/`, `settings/`, `uiux/`

## Domain Layer: UseCases

The domain layer contains all business logic encapsulated in UseCases, each following the **Single Responsibility Principle**. UseCases handle exactly one business operation, return `NanoAIResult<T>` for consistent error handling, receive dependencies via constructor injection, and can be unit tested in isolation.

### Core UseCases by Feature

Feature-owned use cases now sit under `app/src/main/java/com/vjaykrsna/nanoai/feature/*/domain`
with interfaces kept close to their repositories:

**Chat Domain:**
- `LocalInferenceUseCase` + `ChatFeatureCoordinator` for local-first prompts and persona-aware
    routing

**Library Domain:**
- `QueueModelDownloadUseCase`, `PauseModelDownloadUseCase`, `VerifyModelDownloadUseCase`, and
    `ObserveDownloadTasksUseCase` coordinating the download queue and checksum validation

**Settings Domain:**
- `PersonaUseCase` and `BackupUseCase` covering persona switching/restoration and encrypted
    backup/export

**Image & Audio Domains:**
- `ImageGalleryFeatureUseCase` and `AudioSessionCoordinator` encapsulate media flows while
    delegating storage/runtime work to shared `:core` data sources

### Error Handling Architecture

All UseCases return `NanoAIResult<T>` with three possible outcomes:
```kotlin
sealed class NanoAIResult<out T> {
    data class Success<T>(val value: T) : NanoAIResult<T>()
    data class RecoverableError(
        val message: String,
        val telemetryId: String,
        val context: Map<String, String> = emptyMap()
    ) : NanoAIResult<Nothing>()
    data class FatalError(
        val message: String,
        val supportContact: String?,
        val telemetryId: String,
        val cause: Throwable?
    ) : NanoAIResult<Nothing>()
}
```

## Data Layer: Repositories

The data layer provides abstraction over data sources with interface contracts.

### Repository Architecture
- **Interface Contracts**: All repositories have interface definitions
- **Injected Dispatchers**: Coroutine dispatchers provided via dependency injection
- **Consistent Error Handling**: Offline errors propagated through NanoAIResult types
- **Single Responsibility**: Repositories contain only data access logic

### Repository Structure

**Split Repositories (from monolithic ShellStateRepository):**
- `NavigationRepository` - Screen navigation and routing state
- `ConnectivityRepository` - Network connectivity monitoring
- `ThemeRepository` - Theme preferences and Material 3 settings
- `ProgressRepository` - Background operation progress tracking

**Feature Repositories (feature-owned):**
- `DefaultLocalInferenceRepository` (chat) - Installed model discovery + readiness checks
- `ModelDownloadRepository` (library) - Queue, pause/resume, checksum verification, delete
- `PersonaRepository` (settings) - Persona persistence and active persona selection logs
- `BackupRepository` (settings) - Encrypted export/import with warnings surfaced to UI
- `ImageFeatureRepository` / `AudioFeatureRepository` - Media scaffolding while runtimes mature

## Key Data Flows

### AI Conversation Flow
```
User Input
    ↓
ChatScreen (UI)
    ↓
ChatViewModel (orchestrates UI state)
    ↓
SendPromptUseCase + ConversationUseCase (business logic)
    ↓
ConversationRepository + InferenceOrchestrator (data access)
    ↓
Room DB + MediaPipe/Cloud APIs (persistence + external services)
```

### Model Management Flow
```
Model Download Request
    ↓
ModelLibraryScreen (UI)
    ↓
ModelLibraryViewModel (state management)
    ↓
DownloadModelUseCase + ModelCatalogUseCase (business operations)
    ↓
ModelCatalogRepository + WorkManager (data coordination)
    ↓
Room DB + File System + Hugging Face API (storage + external)
```

### User Preferences Flow
```
Settings Change
    ↓
SettingsScreen (UI)
    ↓
SettingsViewModel (validation & state)
    ↓
ApiProviderConfigUseCase + ObserveUserProfileUseCase (business rules)
    ↓
ApiProviderConfigRepository + UserProfileRepository (data persistence)
    ↓
DataStore + Room DB (encrypted storage)
```

## Dependency Injection

Hilt provides clean separation with module-based configuration:

- **DatabaseModule**: NanoAIDatabase, 8 DAOs, type converters
- **NetworkModule**: Retrofit, OkHttpClient, CloudGateway
- **RepositoryModule**: All repository implementations
- **Use Cases**: Constructor-injected business logic
- **ViewModels**: @HiltViewModel with injected dependencies
- **UI**: hiltViewModel() for Compose injection

## Database Schema

Room database with 8 entities supporting offline-first functionality:

- **ChatThread**: Conversation threads with persona/model associations
- **Message**: Individual messages with role, content, and metadata
- **PersonaProfile**: AI personality configurations and preferences
- **PersonaSwitchLog**: Conversation persona change history
- **ModelPackage**: Downloadable AI models with metadata and install state
- **DownloadTask**: Background download progress and status tracking
- **ApiProviderConfig**: Cloud API endpoint configurations
- **UserProfile/UIState**: Cached personalization and layout preferences

- Upcoming schema v9 (chat encryption): the `messages` table will add `ciphertext`, `iv`,
  `encryption_version`, and `search_text` columns while dropping plaintext `text`. Migration will
  encrypt existing rows with an AES/GCM key stored in EncryptedSecretStore and is guarded by
  [app/src/test/java/com/vjaykrsna/nanoai/core/data/db/MessageDaoMigrationTest.kt](app/src/test/java/com/vjaykrsna/nanoai/core/data/db/MessageDaoMigrationTest.kt).

All entities include proper foreign key relationships and indexing for performance.

### ViewModel Architecture

Distributed responsibility pattern ensures clean separation and testability across feature modules.

**Classification:**
- **Feature ViewModels** – Must extend `ViewModelStateHost<S, E>`, exposing a single immutable `StateFlow<UiState>` plus a typed `SharedFlow` of one-off events. Examples: `ChatViewModel`, `ImageGenerationViewModel`, `SettingsViewModel`.
- **Shell/Container ViewModels** – May be plain `ViewModel` subclasses for high-level orchestration and state aggregation. Examples: `AppViewModel`, `ShellViewModel`.

Reducers run on injected dispatchers to keep UI → ViewModel → UseCase interactions predictable and fully testable. ViewModels now exclusively use UseCases for business logic, never calling repositories directly.

### Core Feature ViewModels
- **ChatViewModel**: Manages conversation state and AI interactions
- **ModelLibraryViewModel**: Handles model catalog, downloads, and exports
- **SettingsViewModel**: Manages API configurations and user preferences
- **HuggingFaceLibraryViewModel**: Browses external model catalogs
- **ImageGalleryViewModel**: Manages image operations and gallery

### Navigation & State ViewModels (Distributed)
- **NavigationViewModel**: Screen routing and navigation state
- **ConnectivityViewModel**: Network monitoring and offline handling
- **ProgressViewModel**: Background operation tracking
- **ThemeViewModel**: Theme and accessibility preferences
- **UIStateViewModel**: Screen-specific UI state and caching

### Architecture Benefits
- **Clean Separation**: ViewModels orchestrate UI state, UseCases handle business logic
- **Testability**: `ViewModelStateHost` standardises reducers/events so tests assert entire state snapshots via the shared harness
- **Scalability**: Feature modules evolve independently
- **Maintainability**: Clear boundaries enable safer refactoring
- **Consistency**: Unified state + event exposure enforces the single-source-of-truth model for Compose collectors


## Quality Standards

### UI & Accessibility
- **Accessibility**: WCAG AA compliance with proper touch targets, contrast ratios, and semantic markup
- **Material Design 3**: Consistent theming, spacing, typography, and elevation
- **Performance**: Fast startup and smooth operation targets

### Code Quality
- **Linting**: Automated formatting and static analysis
- **Testing**: High coverage across ViewModel, UI, and Data layers
- **Manual Testing**: Accessibility validation with screen readers

*See `docs/UI_COMPONENTS.md` for detailed UI implementation guidelines.*

## State Management

Reactive flows ensure unidirectional data flow and lifecycle awareness:

- **StateFlow**: UI state (messages, loading states) - survives config changes
- **Flow**: Repository data streams - lazy, cold observables
- **SharedFlow**: One-time events (errors, navigation) - hot, no initial value

UI collects flows via `.collectAsState()` for automatic recomposition and lifecycle management.

## Technical Foundations

### Thread Safety
All operations are main-thread safe:
- **Room**: Coroutine-based DAO operations
- **DataStore**: IO dispatcher for reads/writes
- **ViewModels**: viewModelScope lifecycle management
- **WorkManager**: Background thread execution

### Testing Strategy
Comprehensive test coverage across layers:
- **Unit Tests**: JVM tests for DAO, UseCase, and ViewModel logic
- **Instrumentation**: Device tests for Compose UI, Room operations, and WorkManager
- **Macrobenchmarks**: Performance validation and baseline monitoring

### Performance Monitoring
- **JankStats**: Frame hitch detection and regression monitoring
- **Baseline Profiles**: Optimized startup and navigation paths
- **Metrics State**: Lightweight performance overlays in debug builds

### Accessibility
Semantic markup ensures screen reader compatibility:
- Progress panels with state descriptions and range info
- Landmark navigation and heading hierarchy
- Status announcements for connectivity changes
- Predictive focus order for complex UIs

---
