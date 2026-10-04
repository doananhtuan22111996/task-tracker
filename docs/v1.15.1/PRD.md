# [TaskTracker] PRD — v1.15.1: Play Store Compliance (Edge-to-Edge & R8 Optimization)

**Status**: Approved  
**Version**: 1.15.1  
**Created**: 2026-10-04  
**Author**: Solo Indie Developer  
**Parent Scope**: [v1.15.1 — Play Store Compliance (Edge-to-Edge & R8 Optimization)](https://app.notion.com/p/3efb1772541381358feaca5e55b3d667)

---

## 1. Overview & Problem Statement

Google Play Console reported two action items requiring resolution for Task Tracker:

1. **Edge-to-Edge Display Compliance (Android 15+ / SDK 35/36)**:
   - Android 15 mandates edge-to-edge rendering for apps targeting SDK 35+.
   - While `enableEdgeToEdge()` is called in `MainActivity`, our audit identified that `WidgetConfigureScreen` lacks navigation bar insets on its bottom action bar, causing the "Confirm" button to collide with system 3-button or gesture bars.
   - In addition, manual theme changes (`ThemeMode.LIGHT` / `ThemeMode.DARK`) do not dynamically update system bar icon contrast in edge-to-edge mode.

2. **R8 Performance, Memory & Shrinking Optimization**:
   - Google Play flagged:
     - Low optimization rate (32%)
     - Low obfuscation rate (32%)
     - Low shrinking rate (32%)
     - Optimized resource shrinking isn't enabled
     - Upgrade your Android Gradle plugin to version 9.0 or higher
   - Root cause: `app/proguard-rules.pro` contains overbroad `-keep` rules for Compose (`androidx.compose.**`) and Dagger/Hilt (`dagger.**`). Because these libraries already bundle consumer rules in their AARs and comprise ~68% of the bytecode, these rules explicitly prohibit R8 from optimizing or shrinking them, capping metrics at exactly 32% and inflating the release bundle to ~25 MB.
   - Resource shrinking: The modern unified R8 resource shrinker (`android.r8.optimizedResourceShrinking=true`) is missing in `gradle.properties`.

---

## 2. Goals & Success Metrics

- **Zero Inset Overlaps**: All interactive elements, including `WidgetConfigureScreen`'s bottom Confirm button, clear status bars and navigation bars on both 3-button and gesture navigation.
- **Dynamic Contrast Sync**: Status bar and navigation bar icon colors automatically adapt to the user's selected `ThemeMode` (light icons on dark backgrounds, dark icons on light backgrounds).
- **R8 Optimization Rate Recovery**: Increase R8 optimization, obfuscation, and shrinking rates from 32% to >85%.
- **Bundle Size Reduction**: Reduce release AAB file size from ~25 MB down to ~8–12 MB (>50% size reduction).
- **Modern Resource Shrinking**: Enable `android.r8.optimizedResourceShrinking=true` without missing resources.
- **Build & Quality Stability**: 100% passing JVM unit tests (263+ tests), clean `spotlessCheck`, and successful `bundleRelease`.

---

## 3. Technical Requirements

### 3.1 Edge-to-Edge & System Bars (E2E)
- **E2E-01**: `WidgetConfigureScreen` bottom bar container SHALL apply `Modifier.navigationBarsPadding()` to guarantee bottom clearance.
- **E2E-02**: `TaskTrackerTheme` SHALL synchronize `WindowInsetsControllerCompat.isAppearanceLightStatusBars` and `isAppearanceLightNavigationBars` with the active `darkTheme` state.

### 3.2 R8 & ProGuard Cleanup (R8)
- **R8-01**: Remove blanket Compose keep rules (`-keep class androidx.compose.** { *; }`, `-keep @androidx.compose.runtime.Composable class *`, runtime/interface keeps) relying on Compose's built-in AAR consumer rules.
- **R8-02**: Remove blanket Dagger/Hilt keep rules (`-keep class dagger.** { *; }`, `-keep class javax.inject.** { *; }`, `-keep @javax.inject.Inject class *`) relying on Hilt's Gradle plugin and AAR rules.
- **R8-03**: Retain essential targeted rules:
  - Kotlinx Serialization companion serializers for JSON backup models (`BackupPayload`, `TaskBackupDto`, `SubtaskBackupDto`).
  - Room database implementation rules (`**_Impl`).
  - Crashlytics stack trace preservation (`SourceFile`, `LineNumberTable`).
- **R8-04**: Enable `android.r8.optimizedResourceShrinking=true` in `gradle.properties`.

### 3.3 Build & AGP Roadmap (BUILD)
- **BUILD-01**: Version bump to `1.15.1` (`versionPatch = 1`, `versionCode = 1_789_811_501`).
- **BUILD-02**: Document prerequisites and migration path for future AGP 9.0 / Gradle 9.1 upgrade.

---

## 4. Verification Plan

- `spotlessCheck`: Code formatting validation.
- `testDebugUnitTest`: 263+ JVM unit tests pass.
- `assembleDebug`: Debug build compilation check.
- `bundleRelease`: Release AAB minification, shrinking, and signing verification.
- Output inspection: Confirm release AAB size decreases from ~25 MB to ~8–12 MB.
