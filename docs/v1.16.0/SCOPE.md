# [TaskTracker] Scope — v1.16.0: Release Candidates & Roadmap

**Parent Page**: [Notion: TaskTracker Scope v1.16.0](https://app.notion.com/p/3f5b1772541381e791f4fa56faa23623)  
**Parent Release**: [Notion: v1.16.0](https://app.notion.com/p/3f5b1772541381dbbb12c27ea40c07cc)  
**Date**: 2026-10-10  
**Status**: Draft  
**Author**: Solo Indie Developer  
**Version**: 1.16.0  

---

## TL;DR

Evaluation of top feature candidates for Task Tracker `v1.16.0` following the `v1.15.1` Play Store compliance and R8 release. Evaluates 4 candidate features across user impact, offline-first alignment, Room schema impact, and engineering effort. Strongly recommends **Candidate 1: Smart Shorthand Quick-Parser** paired with **Candidate 2: OS Quick Capture (Quick Settings Tile + Quick-Add Sheet)** under the theme **"Lightning Task Capture"** (zero DB migration, high daily productivity).

---

## 1. Project Baseline & Context

- **Current Version**: `v1.15.1` (commit `b0b4946`, build `1,789,811,501`)
- **Architecture**: Clean Architecture + MVVM, Room DB v12, Jetpack Compose, Material 3, Hilt, WorkManager, Glance Home Screen Widget v2, DataStore, Firebase opt-in diagnostics.
- **Test Suite**: 263+ JVM unit tests using test fakes (`Turbine`, `runTest`), passing in ~26s.
- **Recent Milestones**:
  - `v1.13.0`: Glance Home Screen Widget v2 (2x2/4x2/4x4, interactive completion, source selection).
  - `v1.14.0`: Tablet landscape two-pane calendar split-view (`sw600dp`), agenda archive undo, Widget/Calendar FAQ docs.
  - `v1.15.0`: Inline expandable subtask checklist on task cards & calendar agenda, Android Share Target (`ACTION_SEND`).
  - `v1.15.1`: Play Store edge-to-edge compliance, dynamic system bar contrast syncing, R8 ProGuard rule pruning, release bundle size reduced to 9.1 MB (-63.6%).

---

## 2. Feature Candidates Evaluated

### Candidate 1 (Recommended): Smart Shorthand Quick-Parser
> **Natural shorthand tokenizer parsing dates, times, priority, and tags directly from task title.**
- **Problem Statement**:
  - Setting title, due date, time, priority, and tags takes multiple modal pickers (DatePicker, TimePicker, Priority dropdown, Tag dropdown), taking 5–8 taps per task.
  - Power users expect to type `"Submit tax return tomorrow 3pm !high #finance"` and have the app infer attributes automatically.
- **Proposed Scope**:
  - Natural shorthand syntax tokenizer in pure Kotlin domain layer (`TaskShorthandParser`):
    - Dates: `today`, `tomorrow`, `mon`..`sun` (e.g. `fri`, `monday`), `next week`
    - Times: `at 5pm`, `at 14:30`, `5:00`
    - Priority: `!high` / `!h` / `!3`, `!med` / `!m` / `!2`, `!low` / `!l` / `!1`
    - Tags: `#work`, `#groceries` (auto-matched against existing tags, or creates new tag)
  - Real-time preview suggestion pills below title field in `TaskEditorScreen`.
  - Tapping a pill accepts or dismisses the extracted attribute.
- **Technical Considerations**:
  - **Zero database migrations**: Room DB schema stays at v12.
  - Pure Kotlin domain service with zero Android dependencies.
  - 100% JVM unit testable (`TaskShorthandParserTest`).

---

### Candidate 2 (Recommended Companion): OS Quick Capture (Quick Settings Tile + Quick-Add Sheet)
> **Capture tasks from anywhere in Android without opening the full app.**
- **Problem Statement**:
  - `v1.15.0` delivered Share Target (`ACTION_SEND`), but capturing a thought while using other apps or from the lock screen still requires opening the app launcher.
- **Proposed Scope**:
  - **Quick Settings Tile (`TileService`)**: "Add Task" tile in Android notification shade. 1 tap opens task capture.
  - **Lightweight Quick-Add Bottom Sheet**: Fast bottom sheet with single-line title input, shorthand pill preview, and 1-tap Save or IME Done.
  - Direct shortcut integration allowing instant task creation.
- **Technical Considerations**:
  - **Zero database migrations**.
  - Android framework `TileService` integration (`android.permission.BIND_QUICK_SETTINGS_TILE`).

---

### Candidate 3: Horizontal Tag Filter Bar & Multi-Tag Support
> **1-tap tag filtering on the main task list and multiple tags per task.**
- **Problem Statement**:
  - Each task currently supports only a single tag (`tag: String?`).
  - Users cannot assign multiple contexts (e.g., `#work` and `#urgent`).
- **Proposed Scope**:
  - Multi-tag support per task (`List<String>`).
  - Normalized `tags` and `task_tags` cross-reference table in Room.
  - Horizontal filter chip bar on `TaskListScreen`.
- **Technical Considerations**:
  - **Requires Room database migration (v12 → v13)**.
  - JSON and CSV backup schema bump (v3 → v4 DTO) + migration backward-compatibility.

---

### Candidate 4: Overdue Management & Smart Rescheduling
> **Batch 1-tap carry-over and rescheduling workflow for overdue tasks.**
- **Problem Statement**:
  - Overdue tasks currently accumulate with red indicators without a structured catch-up flow.
- **Proposed Scope**:
  - Dedicated "Overdue Catch-up" prompt or sheet.
  - 1-tap actions: "Reschedule all to Today", "Postpone 1 day", or pick custom date.
- **Technical Considerations**:
  - **Zero database migrations**.
  - High daily utility for habit retention.

---

## 3. Comparison Matrix

| Candidate | User Impact | DB Migration | Est. Effort | Risk |
| :--- | :--- | :--- | :--- | :--- |
| **1. Smart Shorthand Quick-Parser** *(Recommended)* | **High (Daily UX)** | **None (v12)** | **M (6–8h)** | **Low** |
| **2. Quick Settings Tile + Quick-Add Sheet** *(Recommended)* | **High (OS Flow)** | **None (v12)** | **S-M (4–6h)** | **Low** |
| **3. Multi-Tag Support & Filter Bar** | High | Yes (v12 → v13) | L (12–16h) | Medium |
| **4. Overdue Task Rescheduler** | Medium-High | None (v12) | M (6–8h) | Low |

---

## 4. Technical Architecture & Component Design

### 4.1 Domain Shorthand Parser (`TaskShorthandParser`)
```kotlin
data class ParsedTaskTokens(
    val cleanTitle: String,
    val dueAt: Long? = null,
    val dueAtHasTime: Boolean = false,
    val priority: Int? = null,
    val tag: String? = null,
)

object TaskShorthandParser {
    fun parse(rawText: String, clock: Clock = Clock.systemDefaultZone()): ParsedTaskTokens
}
```
- Token patterns:
  - Priority: `!high` / `!h` / `!3` (2), `!med` / `!m` / `!2` (1), `!low` / `!l` / `!1` (0)
  - Tags: `#[A-Za-z0-9_-]+`
  - Dates: `today`, `tomorrow`, `mon`..`sun`, `next week`
  - Times: `at (\d{1,2})(:\d{2})?\s*(am|pm)?`

### 4.2 OS Quick Settings Tile (`QuickAddTaskTileService`)
- Registered with `android.permission.BIND_QUICK_SETTINGS_TILE`.
- Tapping tile sends `ACTION_QUICK_ADD` to `MainActivity` or opens `QuickAddBottomSheet` directly.

### 4.3 UI Layer Integration
- `QuickAddBottomSheet`: Compact single-line input with real-time suggestion pills.
- `TaskEditorScreen`: Live suggestion chips below title field.

---

## 5. Decisions & Rationale

1. **Primary Feature Decision**: Bundle **Candidate 1: Smart Shorthand Quick-Parser** and **Candidate 2: Quick Settings Tile + Quick-Add Sheet** for `v1.16.0` under the theme **"Lightning Task Capture"**.
   - *Rationale*: Zero database migrations keeps the release safe and clean (preserves Room v12 schema). Combining shorthand parsing with a system Quick Settings tile creates a frictionless capture loop from anywhere on Android in under 3 seconds.
2. **Architecture Decision**: Pure Kotlin domain service for shorthand parsing.
   - *Rationale*: Zero Android framework dependencies in `TaskShorthandParser` enables comprehensive JVM unit test coverage without Android runtime mocks.
3. **Deferral Decisions**:
   - Multi-Tag Support (Candidate 3) is deferred to bundle with future database schema migrations cleanly.
   - Overdue Task Rescheduling (Candidate 4) is queued as the next candidate for `v1.17.0`.

---

## 6. Next Steps & Verification Plan

- [ ] User review & alignment on v1.16.0 scope.
- [ ] Create `[TaskTracker] PRD — v1.16.0: Lightning Task Capture (Smart Shorthand & Quick Settings)`.
- [ ] Generate task breakdown (`BREAKDOWN.md`) and Notion tracking tickets.
- [ ] Verification gates: `spotlessCheck`, `testDebugUnitTest` (100% JVM pass), `assembleDebug`, and `bundleRelease`.
