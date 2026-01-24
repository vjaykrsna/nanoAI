# Phase 1 Data Model: Offline Multimodal nanoAI Assistant

This model consolidates core entities for chat, models, privacy, and quality metrics. It is aligned with the current `core` and `feature` packages.

## Core Chat & Inference

### ChatThread
- threadId (UUID)
- title (String?)
- personaId (UUID?)
- activeModelId (String)
- createdAt, updatedAt (Instant)
- isArchived (Boolean)

### Message
- messageId (UUID)
- threadId (UUID)
- role (USER, ASSISTANT, SYSTEM)
- ciphertext (Base64/BLOB, non-null)
- initializationVector (Base64/BLOB, non-null)
- encryptionVersion (Int = 1)
- searchText (String?, redacted preview for search)
- audioUri (String?)
- imageUri (String?)
- source (LOCAL_MODEL, CLOUD_API)
- latencyMs (Long?)
- createdAt (Instant)
- errorCode (String?)
Notes: plaintext `text` is derived at read-time via `MessageCrypto` using an AES-256 key stored in EncryptedSecretStore (alias `nanoai.message`).

### PersonaProfile
- personaId (UUID)
- name, description (String)
- systemPrompt (Text)
- defaultModelPreference (String?)
- temperature, topP (Float)
- defaultVoice, defaultImageStyle (String?)
- createdAt, updatedAt (Instant)

### PersonaSwitchLog
- logId (UUID)
- threadId (UUID)
- previousPersonaId (UUID?)
- newPersonaId (UUID)
- actionTaken (CONTINUE_THREAD, START_NEW_THREAD)
- createdAt (Instant)

## Model Management

### ModelPackage
- modelId (String)
- displayName, version (String)
- providerType (MEDIA_PIPE, TFLITE, MLC_LLM, ONNX_RUNTIME, CLOUD_API)
- sizeBytes (Long)
- capabilities (Set<String>)
- installState (NOT_INSTALLED, DOWNLOADING, INSTALLED, PAUSED, ERROR)
- downloadTaskId (UUID?)
- checksum, signature (String?)
- manifestUrl (URI?)
- deliveryType (LOCAL_ARCHIVE, PLAY_ASSET, CLOUD_FALLBACK)
- minAppVersion (Int?)
- updatedAt (Instant)

### DownloadTask
- taskId (UUID)
- modelId (String)
- progress (Float)
- status (QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED)
- bytesDownloaded (Long)
- startedAt, finishedAt (Instant?)
- errorMessage (String?)

### DownloadManifest
- manifestId (String)
- modelId, version (String)
- checksumSha256 (String)
- sizeBytes (Long)
- downloadUrl (URI)
- expiry (Instant?)
- releaseNotes (String?)

## API & Credentials

### APIProviderConfig
- providerId (String)
- providerName (String)
- baseUrl (String)
- apiKey (Encrypted String)
- apiType (OPENAI_COMPATIBLE, GEMINI, CUSTOM)
- isEnabled (Boolean)
- quotaResetAt (Instant?)
- lastStatus (OK, UNAUTHORIZED, RATE_LIMITED, ERROR, UNKNOWN)

### SecretCredential
- credentialId (String)
- providerId (String)
- keyAlias (String)
- storedAt, rotatesAfter (Instant?)
- scope (TEXT_INFERENCE, VISION, AUDIO, EXPORT)
- metadata (Map<String, String>)

## Privacy & Preferences

### PrivacyPreference
- preferenceId (Int = 1)
- exportWarningsDismissed (Boolean)
- telemetryOptIn (Boolean, default false)
- consentAcknowledgedAt (Instant?)
- disclaimerShownCount (Int)
- retentionPolicy (INDEFINITE, MANUAL_PURGE_ONLY)

### ShellUiPreferences
- preferenceId (Int = 1)
- theme (LIGHT, DARK, SYSTEM)
- density (COMPACT, COMFORTABLE)
- fontScale (Float)
- onboardingCompleted (Boolean)
- dismissedTooltips (Set<String>)

## Runtime & Progress (Non-persistent or Partial)

### ShellLayoutState (runtime)
- windowSizeClass
- isLeftDrawerOpen, isRightDrawerOpen (Boolean)
- activeMode (ModeId)
- showCommandPalette (Boolean)
- connectivity (ConnectivityStatus)
- pendingUndoAction (UndoPayload?)

### RecentActivityItem (runtime)
- id (String)
- modeId (ModeId)
- title (String)
- timestamp (Instant)
- status (COMPLETED, IN_PROGRESS, FAILED)

### ProgressJob (runtime)
- jobId (UUID)
- type (IMAGE_GENERATION, AUDIO_RECORDING, MODEL_DOWNLOAD)
- status (PENDING, RUNNING, PAUSED, FAILED, COMPLETED)
- progress (Float)
- eta (Duration?)
- canRetry (Boolean)
- queuedAt (Instant)

### ImportJob (transient)
- jobId (UUID)
- status (PENDING, RUNNING, COMPLETED, FAILED)
- errorMessage (String?)
- createdAt (Instant)

## Quality & Coverage

### RepoMaintenanceTask
- taskId (String)
- title, description (String)
- category (STATIC_ANALYSIS, SECURITY, TESTING, RUNTIME, DOCS)
- priority (CRITICAL, HIGH, MEDIUM, LOW)
- status (IDENTIFIED, IN_PROGRESS, IN_REVIEW, VERIFIED, BLOCKED)
- owner (String?)
- blockingRules (List<String>)
- linkedArtifacts (List<URI>)
- createdAt, updatedAt (Instant)

### CodeQualityMetric
- metricId (String)
- ruleId (String)
- filePath (String)
- severity (WARNING, ERROR)
- occurrences, threshold (Int)
- firstDetectedAt, resolvedAt (Instant?)
- notes (String?)

### CoverageSummary
- buildId (String)
- timestamp (Instant)
- layerMetrics (Map<TestLayer, CoverageMetric>)
- thresholds (Map<TestLayer, Double>)
- trendDelta (Map<TestLayer, Double>)
- riskItems (List<RiskRegisterItemRef>)

### CoverageTrendPoint
- trendId (String)
- buildId (String)
- layer (VIEW_MODEL, UI, DATA)
- coverage, threshold (Double)
- recordedAt (Instant)

### TestSuiteCatalogEntry
- suiteId (String)
- owner (String)
- layer (TestLayer)
- journey (String)
- coverageContribution (Double)
- riskTags (Set<String>)

### RiskRegisterItem
- riskId (String)
- layer (TestLayer)
- description (String)
- severity (LOW, MEDIUM, HIGH, CRITICAL)
- targetBuild (String?)
- status (OPEN, IN_PROGRESS, RESOLVED, DEFERRED)
- mitigation (String)

## Error Envelope (runtime)

### ErrorEnvelope
- code (NETWORK_UNAVAILABLE, INTEGRITY_FAILURE, AUTH_REQUIRED, OUT_OF_MEMORY, UNKNOWN)
- message (String key)
- cause (Throwable?)
- retryPolicy (RETRYABLE, MANUAL_RETRY, DO_NOT_RETRY)
- telemetryId (String?)
- timestamp (Instant)
- context (Map<String, String>)

## Storage Strategy
- Room: core entities (threads, messages, personas, models, downloads, manifests, maintenance, coverage).
- DataStore/encrypted storage: privacy, UI prefs, credentials.
- Runtime-only: shell state, progress jobs, error envelopes.

Validation rules: enforce referential integrity, valid checksums when present, no plaintext secrets, and coverage metrics within bounds.
