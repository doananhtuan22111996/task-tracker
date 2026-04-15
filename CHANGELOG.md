# Changelog

All notable changes to the Task Tracker app will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com).

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
