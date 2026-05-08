# Changelog

All notable changes to the Task Tracker app will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com).

## [Unreleased]

### Added
- `com.kizitonwose.calendar:compose` dependency (pinned 2.6.2) — month-grid engine for the v1.11.0 Calendar surface (ADR-001, CAL-01)
- `DayDecoration` domain model — per-day aggregate (date, taskCount, priorityBuckets, completedCount, hasRecurringProjection) that drives the calendar month-grid cell renderer (CAL-04)
- `ITaskRepository.observeTasksInRange(start, end)` + `TaskDao` query — reactive stream of tasks whose `dueAt` falls in the half-open window `[start, end)`, including completed, excluding archived; ordered by `dueAt` ascending (CAL-06)
- `RecurrenceCalculator.projectOccurrences(task, windowStart, windowEnd)` — enumerates recurring-task occurrences as `List<LocalDate>` for the inclusive window, honoring `recurrenceEndDate` and a `maxOccurrences` cap (default 200). Pure in-memory; no DB write. Used by the calendar surface to show projected recurrences before a concrete Room row exists (CAL-07)
- `CalendarUseCase.observeMonthDecorations(monthStart, monthEnd)` — aggregates concrete dated tasks and recurrence projections into a reactive `Flow<Map<LocalDate, DayDecoration>>` for the v1.11.0 calendar month grid. Hilt-injected `@Singleton`; honors archive + completion state; `hasRecurringProjection` flag is true only when a day has no concrete row (CAL-05)
- `CalendarViewModel` — Hilt-injected; holds `visibleMonth`/`selectedDay`, exposes `StateFlow<CalendarUiState>` over `CalendarUseCase.observeMonthDecorations`. Events: `onMonthChange(delta)` / `onDaySelect(date)` / `onTodayClick()` / `onJumpToMonth(target)`. Both month and day survive process death via `SavedStateHandle` (ISO string keys `calendar_visible_month` / `calendar_selected_day`) (CAL-08)
- Calendar bottom-nav destination (between Tasks and Stats) + route `calendar` + `CalendarScreen` placeholder scaffold with localized `LLLL yyyy` month title. Enum tab count: 3 → 4. Full 8-locale translations for `nav_tab_calendar` + two placeholder strings (CAL-02, CAL-03)
- `CalendarMonthView` — thin wrapper around Kizitonwose `HorizontalCalendar` (ADR-001) that hides the library types and accepts our domain primitives (`YearMonth`, `LocalDate`, `Map<LocalDate, DayDecoration>`, `(LocalDate) -> Unit`). Auto-scrolls to `visibleMonth` changes. First-day-of-week from device locale. Minimal inline weekday header (CAL-13 will replace) (CAL-11)
- `DayCell` composable — 48dp cell with today-highlight filled circle, selected-highlight border/container, up to 3 descending priority-color dots, dimmed-when-fully-completed alpha, out-of-month opacity, clickable routing to `onDayClick(date)`. Pure-Kotlin `dotsFor`/`hasDotOverflow` helpers with 7 JVM tests (CAL-12)
- Calendar top bar with prev/next chevrons and a Today action. Chevrons call `CalendarViewModel.onMonthChange(±1)`, Today calls `onTodayClick()`. 4 new i18n keys (`action_calendar_today`, `cd_calendar_prev_month`, `cd_calendar_next_month`) × 8 locales (CAL-14)
- Horizontal swipe paging on the calendar month grid. `CalendarMonthView` now seeds `rememberCalendarState` with a `visibleMonth ± 240 months` window, and a `snapshotFlow` observer forwards user-driven swipes to `CalendarViewModel.onJumpToMonth`. Library state persists across month changes (no more regeneration on every VM tick) (CAL-15)
- Day agenda bottom sheet: tapping a calendar day opens a `ModalBottomSheet` with the localized `FULL` date title and a simple list of that day's tasks. Tapping a row opens the task editor. Empty day shows a localized "No tasks" message. Full `TaskItem` visuals (priority stripe, subtask progress) + FAB + multi-select + swipe actions land in follow-up CAL-18..22 tickets. New `CalendarUseCase.observeTasksForDay(day)` + `CalendarUiState.selectedDayTasks` flow piping. 5 new JVM tests (3 use case + 2 ViewModel). 1 new i18n key × 8 locales (CAL-17)
- Day agenda rows now use the full `TaskItem` composable — priority stripe, tag chips, subtask progress indicator, pin icon, overflow menu — matching the main task list exactly. `CalendarViewModel` gained `onToggleTaskComplete` / `onArchiveTask` / `onTogglePin` event handlers routed to `ITaskManager`. `CalendarUiState.subtaskProgress` added (fed by `SubtaskUseCase.observeProgressByTaskId`). 3 new VM tests. Duplicate + skip-occurrence remain on the task list for now (CAL-18)
- Day agenda FAB opens the task editor with `dueDate` prefilled to the selected day. New optional `initialDueAt` query arg on the create route (`task_editor?initialDueAt={epochMillis}`); `TaskEditorViewModel` seeds `_dueAt` when the arg is present in create mode, leaving `dueAtHasTime = false` per PRD OQ-04 decision. 2 new editor-VM tests (seed + sentinel-negative-ignored). 1 new i18n key × 8 locales (CAL-19)
- `AgendaItem` sealed domain model — distinguishes concrete (DB-backed) from projected (recurrence-predicted) calendar rows. Foundation for re-enabling projections in the agenda and grid (ADR-002, CAL-23 part 1)
- `TaskManager.materializeProjectedOccurrence(parentId, date, zone)` — idempotent helper that anchors a recurrence projection to its tapped date as a concrete `tasks` row. Resolves root-vs-child parent ids, preserves `dueAtHasTime` time-of-day, returns the existing row id when the chain already has one on that date. 8 new JVM tests in `TaskManagerRecurrenceTest`. Backing DAO query `findChainTaskOnDate(rootId, start, end)` with 6 contract tests. Not yet wired into the UI — CAL-24 ships that half (CAL-23 part 1)
- `CalendarUseCase.observeAgendaForDay(day, zone)` — emits `Flow<List<AgendaItem>>` merging concrete rows with recurrence projections for a single day. Chain-dedup (concrete suppresses same-day projection of its own chain), parent-only projection (generated children don't re-enumerate), archived + end-date respected. Kept additive: not yet consumed by any screen so the agenda UX stays concrete-only until CAL-24 flips the call site. 8 new JVM tests in `CalendarUseCaseAgendaTest` (CAL-23 part 2)
- Calendar dots now surface projected recurrences again (reverting the CAL-37 band-aid). `CalendarUseCase.observeMonthDecorations` re-enables the projection feed in `buildDecorations` with chain-concrete dedup; `DayDecoration.hasRecurringProjection` flips back to `true` on projection-only days. `CalendarViewModel` consumes `observeAgendaForDay`; `CalendarUiState.selectedDayTasks: List<Task>` → `agendaItems: List<AgendaItem>`. Agenda-row handlers now take `AgendaItem` and materialize `Projected` occurrences via `TaskManager.materializeProjectedOccurrence` before dispatching to the concrete handler (ADR-002 option c — materialize-then-open). New `ProjectedAgendaRow` composable renders projections read-only with an "Upcoming" badge. `DayAgendaSheet` branches on `AgendaItem`. 1 new i18n key × 8 locales. `CalendarUseCase.observeTasksForDay` removed (unused). 10 new VM tests, 3 restored projection-matrix tests in `CalendarUseCaseTest` (CAL-24)
- Day agenda empty state is now polished: EventBusy icon + localized "No tasks on {date}" message inline with the sheet's selected-day title. FAB stays in place so the user can still create a task for the empty day. 1 new i18n key `calendar_agenda_empty_on_date` × 8 locales (CAL-22)
- `WeekdayHeader` composable — reusable month-grid header row with a pure `weekdayShortNames(firstDayOfWeek, locale)` helper. Replaces the inline 19-line lambda in `CalendarMonthView.monthHeader`. Locale-aware short names via `java.time.TextStyle.SHORT`; rotation honors the caller-supplied `firstDayOfWeek` so the header stays column-aligned with the grid. 5 JVM tests covering rotation + locale short-name output (CAL-13)
- TalkBack descriptions for calendar day cells: "{prefix}, {weekday}, {month} {day}, {count} tasks, with high priority, {suffix}" format. Pure `buildDayCellContentDescription` helper composes pre-localized tokens; `DayCell` resolves resources and applies `Modifier.semantics(mergeDescendants = true)`. HIGH priority is announced as a presence indicator, not a count — `priorityBuckets` is a Set, so the true HIGH-task count isn't available at this layer; faking "1 high priority" would mislead users. New i18n: 1 `<plurals>` resource (tasks) + 3 strings (today prefix, selected suffix, has-high-priority) × 8 locales. First use of Android `<plurals>` in the project. Color is no longer the only channel for day-cell state. 10 new JVM tests in `DayCellA11yTest` covering date-only, token combinations, and defensive null paths (CAL-28, CAL-30)
- Calendar month title in `CalendarTopBar` now applies `Modifier.semantics { liveRegion = LiveRegionMode.Polite; heading() }` so TalkBack announces the visible month on every change (chevron tap, Today, swipe paging, out-of-month tap) without stealing focus, and marks the title as a navigable heading. Chevrons already had `contentDescription` (CAL-14); this closes the "what month am I on?" gap for blind users (CAL-29)
- `CalendarEmptyStateCard` — hint card rendered above the month grid when the database holds zero dated tasks. Shows "No dated tasks yet" / "Add a due date to a task to see it on the calendar" + "Add a task" CTA that opens the editor with today's date pre-seeded (reuses the CAL-19 `initialDueAt` path). Driven by new `CalendarUseCase.observeHasAnyDatedTask()` backed by `TaskDao.observeDatedTaskCount()` (O(1) aggregate, no scan). Completed dated tasks still count — once the user has used due-dates the hint stays gone; archived tasks don't count. 3 new i18n keys × 8 locales. 5 new JVM tests on the use case + 1 VM test (CAL-16)
- In-app Help gained a **Calendar** FAQ section with four Q/A (where is the calendar, what the priority dots mean, why future repeating tasks appear before creation, how to add a task for a specific day). Inserted between "Managing Tasks" and "Subtasks" in `HelpScreen`; uses the existing declarative `FaqSection` list so the screen auto-renders. 9 new strings (1 section + 4 Q/A pairs) × 8 locales = 72 entries. Mirrors the RP-03 Subtasks/Batch-Operations FAQ addition pattern from v1.10.0 (CAL-34)

### Changed
- `DayDecoration` marked `@Immutable` so Compose skips `DayCell` recomposition when the instance is structurally equal to its prior value. Previously the embedded `Set<Int>` caused Compose to treat the whole data class as unstable, re-running every cell on every upstream `decorations` map emission. No runtime behavior change — purely a compose-compiler stability hint; `DayDecoration` instances are already produced via immutable `toSet()` snapshots in `CalendarUseCase.buildDecorations` (CAL-31)
- `CalendarUiState.selectedDayTasks: List<Task>` renamed to `agendaItems: List<AgendaItem>` to reflect the mixed concrete/projected contents (CAL-24)

### Removed
- `CalendarUseCase.observeTasksForDay(day, zone)` — replaced by `observeAgendaForDay` which returns the sealed `AgendaItem` type so the UI can render concrete rows and projected previews side-by-side (CAL-24)

### Fixed
- Weekly recurrence with a single-day bitmask and `interval = 1` now correctly advances one week instead of collapsing back to the same day. A Monday-only rule from a Monday used to return the same Monday because both `daysUntilNextMonday` and `weeksToSkip` resolved to zero (caught while implementing CAL-05)

## [1.10.0] - 2026-05-02

### Added
- `Subtask` Room entity with foreign key to `tasks` (cascade delete) and index on `taskId`
- `SubtaskDao` with observe/CRUD/reset-completion operations
- Room migration v11→v12 creating the `subtasks` table
- Android instrumentation test `TaskDatabaseMigrationTest` covering v11→v12 data preservation, schema shape, idempotency, and FK cascade delete
- `androidx.room:room-testing` dependency for migration tests
- `ISubtaskRepository` + `SubtaskRepository` (Hilt-bound) wrapping `SubtaskDao` with transactional reorder
- `SubtaskUseCase` (pure domain) with validation, 500-char title cap, reorder, reset-completion
- `FakeSubtaskRepository` + `TestSubtaskFactory` for JVM unit tests
- `SubtaskUseCaseTest` covering add/update/delete/reorder/reset/observe, Flow reactivity, and CancellationException propagation (24 tests)
- `ISubtaskRepository.copySubtasksResetCompletion(fromTaskId, toTaskId)` — atomic copy of subtasks to a new task with `isCompleted = false`, preserving titles and sortOrder
- Recurrence regeneration (`completeAndGenerateNext` / `archiveAndGenerateNext`) now copies subtasks from the completed/skipped instance to the new instance with checked state reset
- `TaskManagerRecurrenceTest` cases covering subtask copy on complete, skip, no-subtasks no-op, and non-recurring no-op
- Backup schema v3: JSON and CSV backups now include subtasks nested inside each task
- `SubtaskBackupDto` and subtasks field on `TaskBackupDto` (defaulted, backward compatible with v1/v2)
- `ITaskRepository.replaceAllTasksAndSubtasks` — atomic import of tasks + subtasks
- `ISubtaskRepository.getAllSubtasks` — bulk fetch for export
- `ImportBackupUseCase` now restores subtasks, trims oversized titles, and drops blank-title subtasks
- `SubtaskBackupTest` — 7 round-trip tests (JSON + CSV, special characters, v2 backward compat, empty cell)
- `ImportBackupSubtaskTest` — 3 end-to-end tests covering subtask persistence, blank-parent filtering, and oversize-title truncation
- `SubtaskListSection` Composable: inline checklist inside the task editor with checkbox, inline text edit, delete, and "Add subtask" row (IME Done commits)
- Soft-cap hint at 50 subtasks per task (non-blocking advisory text)
- `TaskEditorViewModel` state + events for subtask drafts (add / update title / toggle / remove) with diff-on-save persistence
- English strings for subtasks: `section_subtasks`, `label_add_subtask`, `hint_subtask_soft_cap`, content-description keys for checkbox + remove icon
- 9 new `TaskEditorViewModelTest` cases covering subtask CRUD, validation, hasChanges, save persistence
- Inline subtask progress indicator ("m / n" + slim bar) on each task row in `TaskListScreen` — only rendered when the task has at least one subtask; TalkBack reads "X of Y subtasks completed"
- `SubtaskProgress` projection + `SubtaskDao.observeSubtaskProgress` aggregate query (GROUP BY taskId) for live per-task progress
- `ISubtaskRepository.observeSubtaskProgress` + `SubtaskUseCase.observeProgressByTaskId` for O(1) UI lookup
- English strings `label_subtask_progress`, `cd_subtask_progress`
- 2 new `SubtaskUseCaseTest` cases covering the progress flow (26 total)
- Drag-to-reorder for subtasks in the task editor: long-press the drag handle and move vertically to reorder. Final order is persisted atomically on save.
- English string `cd_drag_handle_subtask` for drag handle content description
- 4 new `TaskEditorViewModelTest` cases covering move / no-op / out-of-bounds / hasChanges propagation (55 total editor tests)
- Translations of all 10 subtasks/progress/drag-reorder strings for `de`, `es`, `fr`, `hi`, `in`, `pt`, `vi` (70 new entries)
- TalkBack-accessible reorder: `CustomAccessibilityAction` "Move up" / "Move down" on each subtask row (with boundary omission — first row has no "Move up", last has no "Move down"); drag remains for sighted users
- English + 7-locale translations for `action_move_up_subtask` / `action_move_down_subtask` (16 new entries)
- Bulk change priority: new overflow action in the selection-mode top bar opens a `BulkPriorityBottomSheet` (Low / Medium / High) that applies the chosen priority to every selected task via `ITaskRepository.setPriorityBulk` and clears the selection
- Back gesture now exits selection mode on `TaskListScreen` (clears selection)
- English strings `action_change_priority`, `cd_change_priority_selected`, `title_change_priority_for_count`, `snackbar_tasks_priority_updated`
- 2 new `TaskBulkActionManagerTest` cases covering bulk priority apply + invalid-priority rejection (22 total)
- Bulk apply tag: new overflow action in the selection-mode top bar opens a `BulkTagBottomSheet` listing existing tags (with color chips) + a "Clear tag" row; applies the chosen tag (or clears) to every selected task via `ITaskRepository.setTagBulk` and clears the selection
- English strings `action_apply_tag`, `action_clear_tag`, `title_apply_tag_for_count`, `cd_apply_tag_selected`, `snackbar_tasks_tagged`, `snackbar_tasks_tag_cleared`
- 3 new `TaskBulkActionManagerTest` cases covering apply / clear / blank-as-clear (25 total)
- Translations of 10 batch-operations strings (BO-07 apply/clear tag + BO-08 change priority) for `de`, `es`, `fr`, `hi`, `in`, `pt`, `vi` — 70 new entries (BO-11)
- In-app help: two new FAQ sections in `HelpScreen` — Subtasks (add, reorder, recurrence reset, parent completion) and Batch Operations (selection mode, undo behavior, why-new-tags-don\'t-appear, exit selection) — translated across all 8 locales (RP-03)

### Changed
- `TaskViewModel` constructor now takes `SubtaskUseCase`
- `TaskListContent` + `TaskItem` gain an optional `subtaskProgressMap` / `subtaskProgress` parameter
- `TaskEditorViewModel` constructor now takes `SubtaskUseCase`
- `TaskManager` constructor now takes `ISubtaskRepository` alongside `ITaskRepository`
- `BackupMetadata.CURRENT_SCHEMA_VERSION` bumped from 2 to 3
- `ExportBackupUseCase` now injects `ISubtaskRepository` to hydrate subtasks during export
- CSV backup header gains a trailing `subtasks` column (JSON-in-cell encoding); import tolerates 13/14/19/20/21 column counts
- Hide the "Apply tag" overflow action in selection mode when no tags exist; also persists sort / priority / tag sheet visibility across rotation via `rememberSaveable`
- `TaskViewModel.availableTags` and `tagColorMap` now derive from a single `TagManagementUseCase.observeTags()` (DB-level DISTINCT) instead of two independent in-memory derivations over `allTasks`. Prevents the bulk-apply tag sheet from missing a brand-new tag due to timing drift between the two flows
- Selection count in the contextual top bar is a `liveRegion = Polite` — TalkBack announces "N selected" as the count changes (BO-12)

### Fixed
- `SubtaskUseCase` mutations now re-throw `CancellationException` instead of wrapping it in `Result.failure`, preserving structured-concurrency cancellation semantics
- Editor no longer silently drops subtask saves — blank-title drafts are skipped on save and any skipped/failed subtask surfaces a user-facing error message
- `AddSubtaskRow` uses `rememberSaveable` so a typed-but-uncommitted subtask draft survives configuration change (rotation)
- Subtask drag gesture no longer uses a stale captured `index` after the first swap — ViewModel re-resolves the current draft position via its stable `id`, so sustained/fast drags reorder correctly
- Subtask drag no longer banks overshoot at list boundaries: `moveSubtaskDraftBy` returns `Boolean`, letting the gesture clamp the accumulator when a move would be a no-op so reverse drags respond immediately

## [1.9.0] - 2026-04-24

### Added
- Tag Management screen: rename, recolor, and delete tags from Settings
- 10-color Material 3 tag palette with per-tag color assignment
- Tag colors displayed on task item chips, filter chips, and tag management list
- Room migration v9→v10 adding `tagColor` column to tasks table
- Tag color auto-inherited when creating/editing tasks with existing tags
- Backup support for tag colors in JSON and CSV formats (backward compatible)
- Tag Management unit tests (12 test cases)
- Full i18n for tag management strings across all 8 locales
- `TagNormalizer` domain helper — canonicalizes tags to trimmed + `Locale.ROOT` uppercase
- Room migration v10→v11 canonicalizing existing tags, merging colors by most-frequent-wins with `MAX(createdAt)` tiebreaker
- `TagCaseMigrationPlanner` pure-Kotlin planner drives the migration so non-ASCII scripts (Vietnamese, Turkish, etc.) canonicalize correctly

### Changed
- All tag input paths (task form, tag rename, JSON/CSV backup import) now normalize tags to uppercase before persisting
- Display-time `.uppercase()` calls removed on task and tag chips — storage is now canonical
- `TaskFormUseCase` no longer normalizes tags — centralizes the invariant at `TaskManager` so the form layer only trims
- New `:app:checkTagInvariant` Gradle task (wired into `:app:check`) fails the build when a file outside the allowlist writes `Task(... tag = ...)` or `.copy(... tag = ...)` directly

### Fixed
- v10→v11 migration uses JVM `Locale.ROOT` uppercasing instead of SQLite `UPPER()` — Android SQLite is ASCII-only, so the previous SQL-based path would have split case variants for non-ASCII tags (`việc`/`Việc`/`VIỆC`)
- `TaskManager.createTask/updateTask/updateTaskContent` now normalize tags via `TagNormalizer` — closes editor bypass where `TaskEditorViewModel` wrote raw lowercase tags straight to the repository
- `TaskEditorViewModel.saveTask` canonicalizes the tag before the `getTagColor` lookup — a lowercase typed variant (`work`) now correctly inherits the color of the existing canonical tag (`WORK`) instead of saving with `tagColor = null`
- `TaskManager.updateTask` / `updateTaskContent` clear `tagColor` whenever the normalized tag is null — prevents orphan tag colors, matching the invariant enforced by the v10→v11 migration and backup import

## [1.8.0] - 2026-04-16

### Added
- Bottom navigation bar with 3 tabs: Tasks, Stats, Archived
- Task filter chip row (All/Active/Completed) replacing old filter bottom bar
- Bottom nav tab labels translated to all 8 locales
- Edge-to-edge list content scrolling behind NavigationBar
- ArchivedScreen TopBar search with 3-mode pattern (normal/search/selection)
- BottomNavBar unit tests for tab config and route matching

### Changed
- Stats and Archived promoted from top bar icons to bottom navigation tabs
- TaskListTopBar simplified to Sort + Settings only
- Stats and Archived screens no longer show back arrow (top-level destinations)
- Tab transitions use crossfade animation
- Tab state preserved via saveState/restoreState when switching
- StatsScreen polished with AppSpacing tokens, semantic colors, section headers
- ArchivedScreen aligned with TaskListScreen M3 patterns (inline tag chips, sticky headers)
- M3 compliance fixes across all screens (tonal elevation, scroll behavior, color tokens)

### Fixed
- Archived empty state now correctly distinguishes "no archived tasks" from "filters hide all results"

### Removed
- Filter-only NavigationBar from TaskListScreen bottom bar
- Stats and Archive icon buttons from TaskListTopBar
- Back arrow from StatsScreen and ArchivedScreen top bars
- Unused SearchField.kt component

## [1.7.0] - 2026-04-13

### Added
- Custom sort picker with bottom sheet UI (due date, created, title, priority)
- Sort preference persistence via DataStore
- "Completed last" toggle for sort grouping
- Sort indicator icon in top bar highlights when non-default sort active
- Room schema export enabled with KSP schema location
- 49 new unit tests (TaskListStateManagerTest, TaskRepositoryTest, SortPersistenceTest)

### Changed
- Migrated `collectAsState()` to `collectAsStateWithLifecycle()` in TaskEditorScreen
- StatsViewModel now refreshes timestamp-dependent counts every 60 seconds
- Backup exclusion rules updated to exclude Room DB from cloud backup

### Fixed
- CompletionRateCard now announces rate via TalkBack (merged semantics)
- RecurrencePicker day chips announce selected/unselected state for screen readers
- Sort options count test updated for new DUE_DATE entry

## [1.6.0] - 2025-04-07

### Fixed
- Comprehensive accessibility fixes for TalkBack support (A11Y-01 to A11Y-13)

## [1.5.0] - 2025-04-06

### Added
- Quick-add: notification Mark Complete action and app shortcut (QA-01 to QA-05)
- Streak tracking for recurring tasks with badge display (ST-01 to ST-07)
- Home screen widget for task list (WG-01 to WG-09)

### Changed
- Translations updated for all 8 locales (en + de/es/fr/hi/in/pt/vi)

## [1.4.0] - 2025-03-30

### Added
- Recurring tasks: recurrence type, interval, days of week, end date
- RecurrenceCalculator with completion, skip, and un-complete flows
- Recurrence editor UI in task create/update
- Repeat icon and skip action in task list
- Recurrence fields in backup serializers (JSON + CSV)
- Recurrence strings translated to 7 locales

## [1.3.1] - 2025-03-25

### Fixed
- Bumped appcompat 1.7.0 → 1.7.1 to remove deprecated edge-to-edge API
- Removed portrait lock and configChanges for large screen support

## [1.3.0] - 2025-03-23

### Added
- Due date time support with optional time picker
- Due date quick-select chips (Today / Tomorrow / Next Week)
- Task duplication from overflow menu
- Stats screen refresh (SPEC-S01–S08)
- Tag chips always-visible row above task list
- Tag filter chip row on archive screen
- Onboarding flow, feature tips, and help screen
- In-app rating prompt and send feedback option
- Comprehensive accessibility audit and TalkBack improvements
- Stats, feedback, accessibility, onboarding, feature tips, and help/FAQ translations

### Changed
- Tasks always ordered by due date (removed manual sort)
- ViewModel unit tests added for TaskViewModel, TaskEditorViewModel, StatsViewModel

## [1.2.0] - 2025-03-15

### Added
- CLAUDE.md with merged workflow and project context

## [1.1.0] - 2025-03-01

### Added
- Import/export tasks (JSON + CSV) via Storage Access Framework
- Backup validation and sanitization
- Comprehensive unit test suite (263 tests)
- Global language support (English + Vietnamese) and theme system
- Settings screen with appearance, language, and backup sections
- Dynamic Color (Material You) support on Android 12+

## [1.0.0] - 2025-02-15

### Added
- Core task management: create, edit, delete tasks
- Search and filter (all / active / completed)
- Sort by created date, title, priority
- Undo delete and confirm delete dialog
- Bulk actions (mark completed, mark active, delete)
- Form validation for task title
- Due dates with push notification reminders
- Notification permission request flow
- Day-based task grouping
- Tags and tag filtering
- Priority levels (low, medium, high)
- Archive and restore tasks
- Stats chart (completion rate, counts)
- Material 3 UI with enhanced styling
- App icon and privacy policy
