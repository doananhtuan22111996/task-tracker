# Changelog

All notable changes to the Task Tracker app will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com).

## [Unreleased]

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

### Added
- Backup schema v3: JSON and CSV backups now include subtasks nested inside each task
- `SubtaskBackupDto` and subtasks field on `TaskBackupDto` (defaulted, backward compatible with v1/v2)
- `ITaskRepository.replaceAllTasksAndSubtasks` — atomic import of tasks + subtasks
- `ISubtaskRepository.getAllSubtasks` — bulk fetch for export
- `ImportBackupUseCase` now restores subtasks, trims oversized titles, and drops blank-title subtasks
- `SubtaskBackupTest` — 7 round-trip tests (JSON + CSV, special characters, v2 backward compat, empty cell)
- `ImportBackupSubtaskTest` — 3 end-to-end tests covering subtask persistence, blank-parent filtering, and oversize-title truncation

### Changed
- `TaskManager` constructor now takes `ISubtaskRepository` alongside `ITaskRepository`
- `BackupMetadata.CURRENT_SCHEMA_VERSION` bumped from 2 to 3
- `ExportBackupUseCase` now injects `ISubtaskRepository` to hydrate subtasks during export
- CSV backup header gains a trailing `subtasks` column (JSON-in-cell encoding); import tolerates 13/14/19/20/21 column counts

### Fixed
- `SubtaskUseCase` mutations now re-throw `CancellationException` instead of wrapping it in `Result.failure`, preserving structured-concurrency cancellation semantics

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
