# nanoAI Agent Rules & Wake-Up Calls

## Active Technologies (main)
- Kotlin 2.2.21 (JDK 17 baseline), Jetpack Compose Material 3, Hilt, WorkManager, Room (SQLite database), DataStore (preferences), Retrofit + Kotlin Serialization, OkHttp, MediaPipe Generative (LiteRT), Coil, Kotlin Coroutines, JUnit5, Detekt, Spotless, Gradle Build Scans, Turbine, planned Roborazzi integration (006-improve-workflow-centralise)

See `gradle/libs.versions.toml` for version details and updates.

## 🚨 Critical Rules for AI Agents

### Architecture Guardians
**NEVER** bypass clean architecture layers. Always route through:
- `UseCase` → `Repository` → `DataSource` (Domain → Data flow)
- `Composable` → `ViewModel` → `UseCase` (UI → Domain flow)
- **Wake-up Call**: Mixing layers creates untestable code and violates the 75/65/70% coverage requirements.

### Testing Imperative
**EVERY** code change requires tests. Targets are non-negotiable:
- ViewModel: ≥75% coverage
- UI: ≥65% coverage
- Data: ≥70% coverage
- **Wake-up Call**: Untested code ships bugs that break offline functionality and accessibility compliance.

### Kotlin-First Purity
**ONLY use Kotlin**. **No Java** interop unless absolutely necessary.
- Use coroutines, not threads
- Use sealed classes for runtime states with associated data (e.g., `NanoAIResult<T>`, UI state hierarchies)
- Use enums for fixed constant sets (e.g., `ModelSort`, `CredentialScope`, database enum columns)
- Use data classes for immutable models
- **Wake-up Call**: Java patterns slow development and miss Kotlin's null-safety advantages.

### Security First
**ALWAYS** classify and protect data according to sensitivity:
- **Secrets** (API Keys): MUST use `EncryptedSecretStore`.
- **Sensitive** (PII, Chat Content): SHOULD be encrypted (e.g., Encrypted Room DB).
- **Non-Sensitive** (UI State, App Config): Can be plaintext (e.g., `ui_preferences`, `privacy_preferences`).
- **Wake-up Call**: Unencrypted storage of secrets or PII risks user privacy - the core value proposition.

### Performance Budgets
**RESPECT** targets:
- Cold start: <1.5s
- Jank: <5% frame drops
- Queue flush: <500ms
- **Wake-up Call**: Poor performance kills user adoption on lower-end Android devices.

## 💀 Common Agent Mistakes to Avoid

### 1. Skipping Use Cases
❌ Direct repository calls from ViewModels
✅ Always create and inject UseCases for business logic
**Why?** UseCases enforce testability and separation of concerns.

### 2. Ignoring Offline Scenarios
❌ Assuming always-online behavior
✅ Test with `TestEnvironmentRule` for offline fallbacks
**Why?** Users expect offline functionality after model downloads.

### 3. Breaking Material 3
❌ Custom styling without Material tokens
✅ Use `MaterialTheme` and semantic colors
**Why?** Inconsistent UX frustrates users and fails accessibility audits.

### 4. Deprecated Dependencies
❌ Using old libraries like RxJava or legacy support
✅ Check `gradle/libs.versions.toml` for current versions
**Why?** Deprecated code bloats APK and introduces security risks.

### 5. Inefficient State Management
❌ MutableState in composables
✅ StateFlow in ViewModels, collectAsState in UI
**Why?** Wrong state management causes UI glitches and memory leaks.

### 6. Blocking UI Thread
❌ Network calls on main thread
✅ Always use coroutines with IO dispatcher
**Why?** ANR crashes destroy user trust.

### 7. Incomplete Error Handling
❌ Silent failures
✅ Proper `NanoAIResult` usage with error propagation
**Why?** Poor errors hide bugs and confuse users.

## ⚡ Quick Action Rules

### When Adding Features
1. Create failing tests first (TDD)
2. Update architecture diagram if changing data flow
3. Test offline + accessibility scenarios

### When Refactoring
1. Update any affected docs in `/docs`
2. Add migration tests for schema changes

### When Debugging
## 📁 Project Structure

```
nanoAI/
├── .github/                       # GitHub workflows, actions, and prompts
├── .specify/                      # Specification tools and templates
├── Personal/                      # Personal files and notes
├── app/                           # 📱 Main Android application (:app)
│   ├── src/main/java/com/vjaykrsna.nanoai/
│   │   ├── MainActivity.kt        # Single activity architecture
│   │   ├── NanoAIApplication.kt   # Application class
│   │   ├── core/                  # Core infrastructure (12 modules)
│   │   │   ├── common/            # Shared utilities & extensions
│   │   │   ├── coverage/          # Test coverage reporting & analysis
│   │   │   ├── data/              # Database, network, repositories
│   │   │   ├── device/            # Camera, storage, hardware access
│   │   │   ├── di/                # Hilt dependency injection
│   │   │   ├── domain/            # Business logic & use cases
│   │   │   ├── maintenance/       # DB migrations & cleanup
│   │   │   ├── model/             # Core enums & type definitions
│   │   │   ├── network/           # HTTP clients & gateways
│   │   │   ├── runtime/           # ML runtime management
│   │   │   ├── security/          # Encryption & hashing
│   │   │   └── telemetry/         # Analytics & error reporting
│   │   ├── feature/               # Feature modules (6 active)
│   │   │   ├── audio/            # Audio processing
│   │   │   ├── chat/             # Chat interface & messaging
│   │   │   ├── image/            # Image operations
│   │   │   ├── library/          # Model catalog & downloads
│   │   │   ├── settings/         # Configuration & privacy
│   │   │   └── uiux/             # Shared UI components
│   │   └── shared/               # Cross-feature utilities
│   ├── src/test/java/             # Unit tests (JVM)
│   └── src/androidTest/java/      # Instrumentation tests (device)
├── build-logic/                   # Custom Gradle plugins & build logic
├── macrobenchmark/                # ⚡ Performance testing (:macrobenchmark)
├── docs/                          # 📚 Documentation & guides
│   ├── api/                       # API documentation
│   ├── architecture/              # System design & data flows
│   └── development/               # Development guides & workflows
├── specs/                         # 🎯 Feature specifications
├── config/                        # ⚙️ Quality gates & configurations
│   ├── quality/                   # Detekt, accessibility, UI quality
│   ├── testing/                   # Coverage metrics & schemas
│   └── build/                     # Build conventions
├── scripts/                       # 🛠️ Dev tools & automation
├── gradle/                        # 🔨 Build system
│   ├── libs.versions.toml         # Single source of truth for deps
│   └── wrapper/                   # Gradle wrapper
├── AGENTS.md                      # Agent rules and guidelines
├── README.md                      # Project overview
├── build.gradle.kts               # Root build script
└── settings.gradle.kts            # Active modules (:app, :macrobenchmark)
```

### When Debugging
1. Check logs with `ShellTelemetry`
2. Isolate layers (UI, Domain, Data)
3. Use `TestEnvironmentRule` for controlled testing
4. Verify on multiple screen sizes/densities

## 🚦 Code Quality Gates

**Must Pass Before Commit:**
- `./gradlew spotlessCheck` (Kotlin formatting)
- `./gradlew detekt` (Static analysis)
- `./gradlew testDebugUnitTest` (Unit tests)
- `./gradlew verifyCoverageThresholds` (Coverage gates)

## 📚 Essential References

- `docs/architecture/ARCHITECTURE.md` - System design and data flows
- `docs/development/TESTING.md` - Coverage requirements and test strategy
- `specs/` - Feature specifications with acceptance criteria
- `gradle/libs.versions.toml` - Approved dependency versions
- `config/testing/coverage/layer-map.json` - Coverage classification rules

## 🛠️ Development Tools & Resources

### When Stuck or Implementing New Features
**USE Context7 MCP** to fetch up-to-date documentation and code examples from official sources:
- For unfamiliar libraries or AI runtimes
- To verify API changes or deprecations
- **Wake-up Call**: Don't guess - always check official docs to avoid deprecated patterns.
- No new accessibility or offline issues
- Architecture diagram remains accurate
- Documentation updated for changes

**Remember**: This is a privacy-first app. Every decision impacts user trust. Test thoroughly, respect performance budgets, and maintain clean architecture.
