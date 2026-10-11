# [TaskTracker] Scope — v1.17.0: Protected with Play & Anti-Abuse Integrity

**Parent Page**: [Notion: TaskTracker v1.17.0](https://app.notion.com/p/3f5b1772541381e99269db0d00bd4693)  
**Parent Release**: [Notion: v1.17.0](https://app.notion.com/p/3f5b1772541381e99269db0d00bd4693)  
**Date**: 2026-10-10  
**Status**: In Progress  
**Author**: Solo Indie Developer  
**Version**: 1.17.0  

---

## TL;DR

Evaluation of security and anti-abuse protections for Task Tracker `v1.17.0` following the `v1.16.0` Lightning Task Capture release. Recommends integrating **Google Play Integrity API** (part of Google Play's **"Protected with Play"** suite) using low-latency Standard Requests, Google Play native remediation dialogs (`GET_LICENSED`), layered local offline fallback heuristics, and a turnkey serverless Firebase Cloud Function verifier. Preserves 100% offline-first functionality with zero database migration risk.

---

## 1. Project Baseline & Context

- **Current Version**: `v1.16.0` (build `1,789,811,600`)
- **Target Version**: `v1.17.0` (build `1,789,811,700`)
- **Architecture**: Clean Architecture + MVVM, Room DB v12, Jetpack Compose, Material 3, Hilt, WorkManager, DataStore.
- **Linked Cloud Project**: `task-tracker-2b75d` (Project #`661684282575`).
- **Core Principles**: Offline-first (local SQLite Room), zero telemetry without opt-in consent, JVM test coverage using Fakes, strict SemVer & Conventional Commits.

---

## 2. Problem Statement & Threat Model

1. **Repackaged / Cloned Binaries (Piracy & Adware Injection)**:
   - Sideloaded modified APKs published on third-party stores often inject malware, spyware, or ad overlays while impersonating Task Tracker.
   - Genuine users deserve assurance that their app binary is unmodified and certified by Google Play.

2. **Unlicensed Sideloading**:
   - Unauthorized distribution bypasses Google Play Store update channels, leaving users on outdated, vulnerable builds.
   - Google Play provides the `GET_LICENSED` remediation dialog to guide users back to the official Play Store listing seamlessly.

3. **Untrusted Runtime Environments**:
   - Malicious screen scrapers and abusive accessibility tools can attempt to inspect or capture sensitive user tasks and notes.
   - Google Play Integrity provides access risk detection (`appAccessRiskVerdict`) to detect unapproved screen recording and remote control tools.

4. **The Offline-First Dilemma**:
   - Play Integrity API tokens are cryptographically encrypted by Google and intended for backend verification.
   - Pure offline-first apps without a dedicated server must not lock out legitimate users who are traveling or offline.

---

## 3. Architecture Evaluation & Key Decisions

### Decision 1: Standard Requests vs Classic Requests
- **Selected**: **Standard Requests** (`StandardIntegrityManager`).
- **Rationale**: Standard requests utilize smart on-device caching, reducing token retrieval latency from seconds to a few hundred milliseconds. Warmed up asynchronously at app startup via `prepareIntegrityToken()`.

### Decision 2: Enforcement Policy (Soft Remediation vs Hard Block)
- **Selected**: **Soft Remediation & User Guidance**.
- **Rationale**:
  - A hard block (crashing or preventing app access) violates Task Tracker's offline-first contract.
  - Sideloaded or unlicensed installs trigger Google Play's official native `GET_LICENSED` remediation dialog (`StandardIntegrityManager.showDialog()`), giving the user a 1-tap pathway to download the genuine app.
  - Local task data in Room DB is **never deleted or locked**.

### Decision 3: Multi-Tiered Verification (Defense in Depth)
- **Tier 1 (Play Integrity SDK)**: In-app token request and Google Play remediation dialogs.
- **Tier 2 (Offline Fallback Heuristics)**: When offline or Play Services is unavailable, check local certificate SHA-256 fingerprint and installer source package (`com.android.vending`).
- **Tier 3 (Turnkey Serverless Verifier)**: Ready-to-deploy Firebase Cloud Function in `scripts/cloud-functions/` for cryptographically verified token attestation via Google Play REST API.

---

## 4. Release Impact Matrix

| Area | Impact | Notes |
| :--- | :--- | :--- |
| **Database Schema** | None (v12 preserved) | Zero migration risk |
| **Offline Functionality** | 100% Preserved | Non-blocking warmup & graceful fallback |
| **Dependency Footprint** | Minimal (+1 library) | `com.google.android.play:integrity:1.6.0` |
| **Test Suite** | 100% JVM Testable | Clean architecture fakes (`FakeIntegrityRepository`) |
| **Localizations** | 8 Locales | All strings in `strings.xml` translated across 8 languages |
