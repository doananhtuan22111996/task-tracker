# [TaskTracker] PRD — v1.17.0: Protected with Play (Play Integrity API)

**Status**: In Progress  
**Version**: 1.17.0  
**Created**: 2026-10-10  
**Author**: Solo Indie Developer  
**Parent Scope**: [docs/v1.17.0/SCOPE.md](file:///Users/tuandoan/s/task-tracker/docs/v1.17.0/SCOPE.md)  

---

## 1. Overview & Problem Statement

Task Tracker `v1.16.0` delivered natural language shorthand task parsing and Quick Settings tile capture. As the app moves further into distribution and closed beta, ensuring app integrity, preventing malicious repackaging, and protecting users against modified cracked binaries is critical.

Integrating **Google Play Integrity API** establishes a verifiable chain of trust between the app binary, the Android operating system, and Google Play, protecting intellectual property and user data without adding server infrastructure burden.

---

## 2. Goals & Success Metrics

- **Zero Database Migrations**: 100% preservation of Room v12 schema (`tasks`, `subtasks`).
- **Zero Impact on Startup Latency**: Asynchronous, background token warmup (`prepareIntegrityToken`) on `Dispatchers.IO` with 0ms main thread blocking.
- **100% Offline Resilience**: Offline users (travelers, airplane mode) never experience modal blocking, lockouts, or task corruption.
- **Official Google Remediation**: Native Google Play `GET_LICENSED` remediation sheet triggered when sideloading or license violations are detected.
- **Clean Architecture & JVM Testability**: Pure Kotlin domain models and use cases with 100% test coverage using JVM test fakes (`FakeIntegrityRepository`).
- **Complete Localization & Accessibility**: TalkBack semantics on security indicators and full string translation across all 8 supported languages (`en`, `de`, `es`, `fr`, `hi`, `in`, `pt`, `vi`).

---

## 3. User Stories

- **US-01 (Genuine Play Verification)**: As a user installing Task Tracker from Google Play, I want the app to seamlessly verify authenticity in the background so that my experience is fast and undisturbed.
- **US-02 (Sideload Remediation)**: As a user running an unverified sideloaded copy from a third-party website, I want to be prompted by Google Play with a 1-tap option to install the official, malware-free version.
- **US-03 (Offline Resilience)**: As an offline user, I want to manage tasks, export backups, and edit settings without being locked out or blocked by missing network connectivity.
- **US-04 (Security Transparency)**: As a privacy-conscious user, I want to view my app's protection status in Settings so I know my install is verified by Google Play Protect.
- **US-05 (Screen Reader Accessibility)**: As a visually impaired user, I want TalkBack to clearly announce the security status in Settings and all dialog buttons.

---

## 4. Functional Requirements

### 4.1 Domain Layer
- **FR-01**: `IntegrityRepository` and `VerifyAppIntegrityUseCase` SHALL reside in `domain/security/` with zero Android framework dependencies.
- **FR-02**: Domain models `IntegrityVerdict`, `AppLicensingStatus`, and `IntegrityCheckResult` SHALL represent all states (licensed, unlicensed, recognized, access risk, offline fallback).

### 4.2 Play Integrity SDK Integration
- **FR-03**: The app SHALL use `com.google.android.play:integrity:1.6.0` configured via `gradle/libs.versions.toml`.
- **FR-04**: `PlayIntegrityRepositoryImpl` SHALL warm up the token provider during app launch via `StandardIntegrityManager.prepareIntegrityToken()` with Google Cloud Project Number `661684282575L`.
- **FR-05**: `StandardIntegrityManager.showDialog()` SHALL be invoked with `IntegrityDialogTypeCode.GET_LICENSED` (code 1) when license violations occur.

### 4.3 Offline Fallback & Heuristics
- **FR-06**: When device is offline or Google Play Services is unavailable, the repository SHALL execute local heuristic validation (package manager installer check `getInstallSourceInfo` and signing certificate fingerprint verification).
- **FR-07**: Local offline checks SHALL never crash, throw unhandled exceptions, or block core task management features.

### 4.4 UI & Diagnostics
- **FR-08**: `SettingsScreen` SHALL display an "App Protection" status row in the Security section with an icon and localized description.
- **FR-09**: `HelpScreen` SHALL include an FAQ entry explaining "Play Protect & App Integrity".

### 4.5 Serverless Token Verifier
- **FR-10**: The project SHALL provide a Node.js Firebase Cloud Function in `scripts/cloud-functions/verify-integrity/` capable of decoding integrity tokens via `playintegrity.googleapis.com`.
