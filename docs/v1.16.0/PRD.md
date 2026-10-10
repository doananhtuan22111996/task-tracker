# [TaskTracker] PRD — v1.16.0: Lightning Task Capture (Smart Shorthand & Quick Settings)

**Status**: Approved  
**Version**: 1.16.0  
**Created**: 2026-10-10  
**Author**: Solo Indie Developer  
**Parent Scope**: [docs/v1.16.0/SCOPE.md](file:///Users/tuandoan/s/task-tracker/docs/v1.16.0/SCOPE.md)  

---

## 1. Overview & Problem Statement

Task Tracker `v1.15.0` and `v1.15.1` delivered inline subtask checklists, Android share sheet capture, edge-to-edge compliance, and a 63.6% release bundle reduction. However, fast task capture and entry still have friction:

1. **Task Entry Input Friction (CAP-10)**:
   - Creating a task with a due date, time, priority, and tag currently requires tapping through 4 separate modal pickers (DatePicker, TimePicker, Priority dropdown, Tag dropdown), taking 5–8 taps per task.
   - Users creating tasks during their workday expect to type phrases like `"Submit tax report tomorrow 3pm !high #finance"` and have the app intelligently extract due dates, times, priority levels, and tags.

2. **External Capture Latency (CAP-12)**:
   - While `ACTION_SEND` captures links from external apps, creating a quick task while using the phone or from the lock screen still requires navigating to the home screen, opening Task Tracker, waiting for launch, and tapping the FAB.
   - Android's native Quick Settings Tile (`TileService`) enables 1-tap capture from the notification shade, anywhere in the OS.

---

## 2. Goals & Success Metrics

- **Zero Database Migrations**: Full preservation of Room v12 schema (`tasks`, `subtasks`). Zero migration risk.
- **<3-Second Task Creation**: Quick Settings Tile + Quick-Add sheet allows typing a task and saving in under 3 seconds.
- **Smart Shorthand Precision**: Shorthand parser extracts dates, times, priorities, and tags with 0 false positive title corruption. Original title is safely preserved if tokens are ambiguous.
- **100% JVM Test Coverage**: Pure Kotlin domain parser with 100% unit test coverage using JVM tests (no Android mocks required).
- **Accessibility & i18n**: 100% TalkBack semantic coverage on suggestion pills and Quick Settings tile; full string localization across all 8 supported languages (`en`, `de`, `es`, `fr`, `hi`, `in`, `pt`, `vi`).
- **Production Stability**: Crash-free rate $\ge 99.5\%$, clean `spotlessCheck`, and successful `assembleDebug` / `bundleRelease`.

---

## 3. User Stories

- **US-01 (Shorthand Typing)**: As a user in the task editor, I want to type shorthand syntax like `"Call dentist Friday 10am !high #health"` and see real-time suggestion pills showing extracted date, time, priority, and tag so I don't have to open separate pickers.
- **US-02 (Pill Confirmation)**: As a user reviewing parsed suggestions, I want to tap on a suggestion pill to accept it into the form or dismiss it if I meant it as literal title text.
- **US-03 (Quick Settings Capture)**: As a user browsing or using another app, I want to swipe down the notification shade and tap "Add Task" to immediately open a compact quick-add sheet without launching the full app UI.
- **US-04 (Quick-Add Sheet UX)**: As a user in the Quick-Add sheet, I want to type a title with shorthand, press keyboard `Done`, and have the task saved immediately with the sheet auto-dismissing.
- **US-05 (Accessibility & Screen Readers)**: As a visually impaired user, I want TalkBack to announce parsed suggestion pills, announce when tokens are extracted, and clearly label the Quick Settings tile.

---

## 4. Functional Requirements

### 4.1 Domain Shorthand Parser (CAP-10)
- **FR-01**: `TaskShorthandParser` SHALL be a pure Kotlin service residing in `domain/service/` with zero Android framework dependencies.
- **FR-02**: `TaskShorthandParser.parse(text: String, clock: Clock)` SHALL extract:
  - **Due Date**: Relative keywords (`today`, `tomorrow`, `next week`) and day names (`monday`, `mon`, `tuesday`, `tue`, `wednesday`, `wed`, `thursday`, `thu`, `friday`, `fri`, `saturday`, `sat`, `sunday`, `sun`).
  - **Time**: 12h/24h formats (`at 5pm`, `at 5:30pm`, `at 17:00`, `9am`, `14:30`).
  - **Priority**: `!high` / `!h` / `!3` (High = 2), `!med` / `!medium` / `!m` / `!2` (Medium = 1), `!low` / `!l` / `!1` (Low = 0).
  - **Tag**: `#[A-Za-z0-9_-]+` (normalized via `TagNormalizer`).
- **FR-03**: `TaskShorthandParser` SHALL return `cleanTitle` with recognized tokens removed, whitespace trimmed, and punctuation normalized. If cleaning would empty the title, the original input is preserved.

### 4.2 UI Suggestion Pills in Task Editor (CAP-11)
- **FR-04**: `TaskEditorScreen` SHALL display an animated row of suggestion chips (`SuggestionChip` / `AssistChip`) directly below the title input when valid shorthand tokens are recognized.
- **FR-05**: Each pill SHALL display an icon and readable label:
  - Date/Time: `📅 [Formatted Date/Time]`
  - Priority: `🔺 [Priority Label]`
  - Tag: `🏷️ #[Tag Name]`
- **FR-06**: Tapping a suggestion pill applies the value to the form state (e.g. updates `dueAt`, `priority`, or `tag`) and strips the token from the title field.
- **FR-07**: Tapping an individual pill's dismiss action (or clearing the title token) dismisses the suggestion without altering the field.

### 4.3 OS Quick Settings Tile (CAP-12)
- **FR-08**: `AndroidManifest.xml` SHALL declare `QuickAddTaskTileService` extending `TileService` with `android.permission.BIND_QUICK_SETTINGS_TILE`.
- **FR-09**: The tile SHALL display a clean "Add Task" icon (`ic_task_add`) and localized label `quick_add_tile_label`.
- **FR-10**: Clicking the tile SHALL launch the quick-add flow via `Intent.ACTION_VIEW` / custom action `dev.tuandoan.tasktracker.ACTION_QUICK_ADD` to open the quick capture dialog directly.

### 4.4 Lightweight Quick-Add Bottom Sheet (CAP-13)
- **FR-11**: `QuickAddBottomSheet` SHALL render as a compact Material 3 sheet with:
  - Single-line title input field with auto-focus and IME `Done`.
  - Live shorthand suggestion pills beneath the input.
  - 1-tap "Save" icon button.
- **FR-12**: Submitting via IME `Done` or Save button applies all recognized shorthand tokens, saves the task to Room via `TaskCrudUseCase`, and dismisses the sheet in a single interaction.

### 4.5 Localization & Help FAQ (REL-17)
- **FR-13**: All new UI strings SHALL be localized across all 8 supported languages (`en`, `de`, `es`, `fr`, `hi`, `in`, `pt`, `vi`).
- **FR-14**: `HelpScreen` SHALL add FAQ entries explaining shorthand syntax tokens and Quick Settings tile usage.

---

## 5. Non-Functional Requirements

- **NFR-01 (Zero Schema Migration)**: Room database version remains 12. No migrations required.
- **NFR-02 (Performance)**: Token parsing occurs asynchronously or debounced (150ms) to guarantee zero typing latency on low-end devices.
- **NFR-03 (Offline-First)**: 100% offline operation. All tasks persist directly to Room.
- **NFR-04 (Accessibility)**: Full TalkBack content descriptions and state semantics for all interactive pills and tile elements.
- **NFR-05 (Build & Shrinking)**: Zero R8 regressions; release bundle size remains $\le 10$ MB.
