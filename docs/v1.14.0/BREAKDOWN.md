# [TaskTracker] Breakdown — v1.14.0: Tablet Split-Pane + Calendar Polish & FAQ

**Parent PRD**: [docs/v1.14.0/PRD.md](file:///Users/tuandoan/s/task-tracker/docs/v1.14.0/PRD.md)  
**Total Estimate**: ~12–18 hours solo  
**Target Release**: v1.14.0  

---

## Epics & Detailed Task Breakdown

### Epic A: Tablet Landscape Split-Pane (CAL-25)

```markdown
- [x] CAL-25.1: Integrate WindowSizeClass in App & Screen Layer
  - Type: feat
  - Estimate: S (1–2h)
  - Files:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - MainActivity.kt
    - CalendarScreen.kt
  - Description: Add `material3-window-size-class` to project dependencies. Compute `WindowWidthSizeClass` in `MainActivity` and thread `windowWidthSizeClass` (or retrieve via composition local / parameter) to `CalendarScreen`.

- [x] CAL-25.2: Extract Reusable DayAgendaContent Composable
  - Type: refactor
  - Estimate: S (1–2h)
  - Files:
    - app/src/main/java/dev/tuandoan/tasktracker/ui/components/DayAgendaContent.kt (NEW)
    - app/src/main/java/dev/tuandoan/tasktracker/ui/components/DayAgendaSheet.kt
  - Description: Extract the inner content of `DayAgendaSheet` (title/selection bar, task list / projected rows, empty state, and FAB) into a standalone `DayAgendaContent` composable. `DayAgendaSheet` becomes a thin wrapper around `DayAgendaContent` inside `ModalBottomSheet`.

- [x] CAL-25.3: Implement CalendarScreen Two-Pane Layout
  - Type: feat
  - Estimate: M (3–4h)
  - Files:
    - app/src/main/java/dev/tuandoan/tasktracker/ui/screens/CalendarScreen.kt
  - Description: Branch layout based on `WindowWidthSizeClass.Expanded` (or `Medium` in landscape). When two-pane is active:
    - Left pane (50%): Month top bar, empty state hint (if any), and `CalendarMonthView`.
    - Right pane (50%): `Surface` holding `DayAgendaContent`.
    - Selected day anchoring: if visible month contains `selectedDay`, display agenda. If `selectedDay` is outside visible month, show prompt to select a day.
    - Suppress `ModalBottomSheet` when two-pane is active; day taps simply update `viewModel.onDaySelect(date)`.

- [x] CAL-25.4: Tablet Accessibility & TalkBack Landmark Polish
  - Type: feat
  - Estimate: S (1h)
  - Files:
    - app/src/main/java/dev/tuandoan/tasktracker/ui/screens/CalendarScreen.kt
  - Description: Add `semantics` landmarks (`Role.Landmark` / headings) to both panes so TalkBack announces "Calendar month grid" and "Day agenda" clearly without stealing focus.
```

---

### Epic B: Calendar Single-Task Archive Undo (CAL-21)

```markdown
- [x] CAL-21.1: Wire Single-Task Archive Undo in CalendarViewModel
  - Type: feat
  - Estimate: S (1–2h)
  - Files:
    - app/src/main/java/dev/tuandoan/tasktracker/ui/viewmodel/CalendarViewModel.kt
  - Description: In `CalendarViewModel.onAgendaItemArchive`, resolve the concrete task, archive via `taskManager.archiveTask(task.id)`, and emit `UiEvent.ShowUndoArchive(tasks = listOf(task), onUndo = { taskManager.unarchiveTask(task.id) }, message = context.getString(R.string.snackbar_task_archived))` on `agendaUiEvent`.

- [x] CAL-21.2: ModalBottomSheet & Scaffold Snackbar Visibility Fix
  - Type: fix
  - Estimate: M (2–3h)
  - Files:
    - app/src/main/java/dev/tuandoan/tasktracker/ui/components/DayAgendaSheet.kt
    - app/src/main/java/dev/tuandoan/tasktracker/ui/components/DayAgendaContent.kt
    - app/src/main/java/dev/tuandoan/tasktracker/ui/screens/CalendarScreen.kt
  - Description: Ensure the snackbar is fully visible when a task is archived from the bottom sheet:
    - Provide a `SnackbarHost(snackbarHostState)` positioned above bottom navigation / bottom sheet, or inside `DayAgendaSheet`, ensuring the snackbar does not render hidden behind the modal surface.
    - Test on phone and tablet viewports.

- [x] CAL-21.3: JVM Unit Tests for CalendarViewModel Single-Task Archive
  - Type: test
  - Estimate: S (1–2h)
  - Files:
    - app/src/test/java/dev/tuandoan/tasktracker/ui/viewmodel/CalendarViewModelTest.kt
  - Description: Add tests verifying `onAgendaItemArchive` invokes `taskManager.archiveTask`, emits `ShowUndoArchive`, and invoking `onUndo` triggers `taskManager.unarchiveTask`.
```

---

### Epic C: Help & FAQ Polish for Widgets and Multi-Select (V13-26)

```markdown
- [x] V13-26.1: Add WIDGETS to HelpFaqSection & Strings
  - Type: feat / i18n
  - Estimate: S (1–2h)
  - Files:
    - app/src/main/java/dev/tuandoan/tasktracker/diagnostics/AnalyticsLogger.kt (`HelpFaqSection.WIDGETS`)
    - app/src/main/res/values/strings.xml
  - Description: Add `WIDGETS("widgets")` to `HelpFaqSection`. Add English baseline strings for:
    - `help_section_widgets`
    - `help_q_widget_sizes` & `help_a_widget_sizes` (2x2, 4x2, 4x4)
    - `help_q_widget_configure` & `help_a_widget_configure` (sources: Today, Upcoming 7d, Pinned, Tag)
    - `help_q_widget_complete` & `help_a_widget_complete` (completing from home screen)
    - `help_q_agenda_multi_select` & `help_a_agenda_multi_select` (long-press selection on calendar agenda)

- [x] V13-26.2: Wire New FAQ Sections in HelpScreen
  - Type: feat
  - Estimate: XS (<1h)
  - Files:
    - app/src/main/java/dev/tuandoan/tasktracker/ui/screens/HelpScreen.kt
  - Description: Wire the new `WIDGETS` section and calendar agenda multi-select questions into `faqSections` in `HelpScreen.kt`.

- [x] V13-26.3: Localize New FAQ Strings Across 7 Locales
  - Type: i18n
  - Estimate: M (2–3h)
  - Files:
    - app/src/main/res/values-de/strings.xml
    - app/src/main/res/values-es/strings.xml
    - app/src/main/res/values-fr/strings.xml
    - app/src/main/res/values-hi/strings.xml
    - app/src/main/res/values-in/strings.xml
    - app/src/main/res/values-pt/strings.xml
    - app/src/main/res/values-vi/strings.xml
  - Description: Translate all new strings to `de`, `es`, `fr`, `hi`, `in`, `pt`, `vi` maintaining exact key parity.
```

---

### Epic D: Release Preparation & Verification

```markdown
- [x] V14-01: Regression Testing & Formatting
  - Type: test
  - Estimate: S (1h)
  - Commands:
    - `./gradlew spotlessApply`
    - `./gradlew testDebugUnitTest`
    - `./gradlew assembleDebug`
  - Description: Verify all 1,062+ existing tests pass alongside new tests, spotless check is clean, and debug build succeeds.

- [x] V14-02: Bump Version & Update Documentation
  - Type: chore
  - Estimate: XS (<1h)
  - Files:
    - app/build.gradle.kts (`versionMinor = 14`, `versionPatch = 0`)
    - CHANGELOG.md (add `[1.14.0] - YYYY-MM-DD`)
```

---

## Execution Order & Dependency Graph

```
CAL-25.1 (WindowSizeClass dep)
  ↓
CAL-25.2 (Extract DayAgendaContent) → CAL-25.3 (CalendarScreen Two-Pane) → CAL-25.4 (TalkBack)
                                         ↓
CAL-21.1 (VM Archive Undo) → CAL-21.2 (Snackbar Layering) → CAL-21.3 (VM Tests)
  ↓
V13-26.1 (FAQ Strings en) → V13-26.2 (HelpScreen wiring) → V13-26.3 (7-locale i18n)
  ↓
V14-01 (Full Test & Spotless) → V14-02 (Release Bump)
```
