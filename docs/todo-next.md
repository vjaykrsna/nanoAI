# Next Development Phase: Post-Coverage Initiative

Based on the completed 005-improve-test-coverage feature and codebase analysis, the nanoAI project now has a robust test foundation with automated quality gates. This document outlines the next development priorities, focusing on reaching target coverage thresholds, expanding multimodal capabilities, and continuing UX polish.

**Last Updated**: 2025-10-14 (T019-T022 Implementation Complete)

## Phase 1: Coverage Threshold Achievement (High Priority)

### Closing the Gap
The current coverage snapshot (2025-10-12) shows significant gaps from target thresholds:
- ViewModel: 39.58% → **Target 75%** (35.42pp gap)
- UI: 1.90% → **Target 65%** (63.10pp gap)  
- Data: 18.91% → **Target 70%** (51.09pp gap)

### Immediate Actions
- [ ] **Expand ViewModel test coverage**: Focus on high-traffic ViewModels (`ChatViewModel`, `MessageComposerViewModel`, `HistoryViewModel`, `ModelLibraryViewModel`) with comprehensive unit tests covering state transitions, error handling, and offline scenarios.
- [ ] **Increase UI test coverage**: Add instrumentation tests for critical Compose screens and components that lack coverage (Home Screen, Chat Interface, Model Library, Settings), ensuring TalkBack semantics and accessibility compliance.
- [ ] **Strengthen Data layer tests**: Expand Room DAO tests, Repository tests with MockWebServer scenarios, and DataStore preference tests to validate offline-first behavior and data persistence.
- [ ] **Address failing test scenarios**: Resolve incomplete implementations flagged in risk register (RR-CRIT-041, RR-HIGH-027, RR-HIGH-033) to bring tests to passing state.
- [ ] **Enable full CI coverage**: Ensure CI runs `./gradlew ciManagedDeviceDebugAndroidTest jacocoFullReport` with hardware virtualization to collect complete unit + instrumentation coverage (not just unit-only fallback).

### Coverage-Related Improvements
- [ ] Refine layer classification map (`config/coverage/layer-map.json`) as new modules are added.
- [ ] Add telemetry for coverage trend monitoring (already scaffolded in `CoverageTelemetryReporter`).

## Phase 2: Resolved Flows & Test Infrastructure


### Deferred Items (from Phase 005)
- **TODO**: Fix pre-existing androidTest compilation errors (unresolved references in ModelLibraryScreenTest, SettingsScreenTest, ComposeTestHarness) blocking instrumentation coverage collection.
- **TODO**: Instrumentation coverage collection on full CI (currently unit-only due to virtualization constraints locally).
- **TODO**: Additional schema validation for coverage JSON in contract tests.
- **TODO**: Performance macrobenchmarks for coverage dashboard warm-load (<100ms target).
- **TODO**: Automated risk escalation notifications when Critical risks remain open past target build.

## Phase 3: Multimodal & Advanced Features (Medium Priority)

Building on the solid test foundation, these features can now be developed with proper TDD:

- [ ] **Integrate image input/output**: Add camera/gallery picker in chat; support vision models (MediaPipe Vision or cloud APIs). Include comprehensive tests for image handling, caching, and offline scenarios.
- [ ] **Enable audio I/O**: Implement speech-to-text and TTS output with accessibility considerations. Test permission flows, audio playback, and offline fallbacks.
- [ ] **Expand runtimes**: Integrate TensorFlow Lite or MLC LLM as secondary local options. Abstract via `LocalModelRuntime` interface with test doubles for each runtime.
- [ ] **Add image generation**: Research on-device Stable Diffusion; integrate cloud options. Create test fixtures for generation scenarios and error handling.
- [ ] **Implement concurrent models**: Allow multiple active inferences with resource monitoring. Test OOM scenarios and graceful degradation.

## Phase 4: UX Polish & Accessibility (Medium Priority)

- [ ] **Enhance onboarding**: Multi-step flow with interactive demos and feature highlights (building on disclaimer implementation).
- [ ] **Accessibility audit**: Full WCAG 2.1 AA compliance check with automated tests; extend TalkBack coverage to dynamic content.
- [ ] **Theme & density refinements**: Support expanded visual density; add custom accent color picker; test theme persistence across process death.
- [ ] **Sidebar enhancements**: Add badges, drag-to-reorder, collapsible sections with proper semantic markup and testing.
- [ ] **Error & offline UX**: Progressive enhancement for queued actions; refine retry animations and offline banners based on instrumentation test findings.

## Phase 5: Performance & Scalability (Low Priority)

- [ ] **Integrate Baseline Profiles**: Target <2s local response, <300ms FMP with macrobenchmark validation.
- [ ] **Security audit**: Encrypt exports optionally; validate imports against schema; test credential masking in logs/UI.
- [ ] **Multi-device sync**: Add optional cloud sync with E2E encryption (requires extensive security testing).
- [ ] **Community models**: Expand library with user-submitted models; add validation workflow with safety tests.
- [ ] **Analytics (opt-in)**: Add privacy-safe local metrics for crashes/performance; no PII, extensive privacy testing.

## Phase 6: Technical Debt & Maintenance

- [ ] **Room/DataStore migration plan**: Clean up legacy data fields removed during feature development (onboarding, dismissed tips).
- [ ] **Detekt baseline refresh**: Address new findings after coverage expansion; update spotless configuration.
- [ ] **Dependency updates**: Regular Context7 MCP checks for library updates; maintain compatibility with AGP/Kotlin versions.
- [ ] **Documentation updates**: Keep API docs, architecture diagrams, and quickstart guides current as features evolve.

## Success Metrics

### Coverage Targets (3-month horizon)
- ViewModel: 39.58% → 75% (by end of Q4 2025)
- UI: 1.90% → 65% (by end of Q4 2025)
- Data: 18.91% → 70% (by end of Q4 2025)

### Quality Gates
- All Critical risks resolved before any production release
- Maximum 2 High-severity risks in-flight at any time
- CI green rate >95% over rolling 30-day window
- Test execution time <20 minutes for full suite

### Development Velocity
- TDD adoption: All new features start with failing tests
- Code review SLA: Tests reviewed within 24 hours
- Documentation debt: All new public APIs documented before merge

## Risks & Dependencies

### Technical Risks
- **Hardware variance**: Multimodal features require extensive testing on low-RAM devices (Pixel 4a+).
- **Flaky tests**: Continue monitoring offline test stability; use emulator mocking for network scenarios.
- **CI resources**: Full instrumentation coverage requires hardware virtualization; ensure CI runners support it.

### Schedule Risks
- Coverage gap closure: Estimated 4-6 weeks of focused effort (could delay multimodal work).
- Risk mitigation: Some items target builds 2-3 sprints out; monitor for slippage.

### Dependencies
- JUnit6 migration complete (unlocks better test parameterization and extensions)
- Coverage tooling operational (enables continuous threshold monitoring)
- Managed device configuration stable (supports deterministic CI runs)

### Critical Issues
1. **Instrumentation Compilation**: MockK dependency conflicts blocking AndroidTest execution
2. **Large Test Classes**: `ModelCatalogRepositoryImplTest`, `SettingsScreenTest`, `ModelLibraryScreenTest` exceed complexity limits
3. **Test Organization**: Mixed patterns, inconsistent mocking, lack of shared fixtures
4. **Execution Reliability**: Device dependency issues and flaky async tests

## How to Update This Document

1. Review quarterly or when a major feature phase completes.
2. Move completed items to "Successfully Completed" section with checkmarks.
3. Add new items discovered during sprint planning or coverage reviews.
4. Update success metrics based on actual coverage reports from `./gradlew jacocoFullReport`.
5. Cross-reference with `docs/coverage/risk-register.md` for risk-driven priorities.
6. Commit updates alongside related feature specs or architecture changes.

---

**Related Documentation**
- [Testing & Coverage Guide](testing.md)
- [Coverage Risk Register](coverage/risk-register.md)
- [Feature Spec: 005-improve-test-coverage](../specs/005-improve-test-coverage/spec.md)
- [Architecture Overview](ARCHITECTURE.md)
