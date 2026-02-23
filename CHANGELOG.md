# Changelog

All notable changes to the Task Tracker app will be documented in this file.

## [Unreleased]

### Added
- **Theme system** - Light, Dark, and System default theme modes with persistence via DataStore preferences.
- **Dynamic Color** - Material You wallpaper-based color support on Android 12+, with graceful fallback on older devices.
- **Per-app language selection** - In-app language picker (English, Vietnamese) using AppCompatDelegate for all API levels.
- **Settings screen redesign** - Appearance, Language, and Backup sections with full localization (no hardcoded strings).
- **Unit tests** - Added SettingsViewModel tests covering theme, dynamic color, language, and backup/restore state management.

### Changed
- **Localized UI strings** - Replaced all hardcoded user-facing strings in 14 Compose UI files with `stringResource(R.string.xxx)` calls to enable full multi-language support. Covers screens (TaskList, Archived, Stats, TaskEditor) and components (TopBars, SortMenu, EmptyStates, SearchField, FilterTabs, TaskItem, ArchivedTaskItem, ArchivedTaskListContent, NotificationPermissionDialog).
- **Localized non-UI strings** - Replaced hardcoded strings in domain/ViewModel/utility files that lack Compose context, using `context.getString(R.string.xxx)` pattern. Covers TaskTrackerApplication, TaskReminderWorker, TaskDateGrouper, TaskFormUseCase, TaskEditorViewModel, TaskViewModel, TaskBulkActionManager, and SettingsViewModel. Injected `@ApplicationContext Context` where needed; updated TaskDateGrouper to accept Context parameter.
- **Localized CRUD/backup strings** - Replaced all remaining hardcoded user-facing strings in TaskCrudUseCase, TaskCrudManager, TaskFormStateManager, ExportBackupUseCase, ImportBackupUseCase, and NotificationPermissionManager with `context.getString(R.string.xxx)` calls. Injected `@ApplicationContext Context` into classes that needed it. Updated corresponding unit tests (TaskCrudUseCaseTest, TaskCrudManagerValidationTest, TaskBulkActionManagerTest) to provide mock Context.
- **MainActivity** - Switched from ComponentActivity to AppCompatActivity to support per-app locale changes.
- **TaskTrackerTheme** - Now accepts ThemeMode enum instead of Boolean darkTheme parameter.
- **SettingsViewModel** - Extended with theme, dynamic color, and language management alongside existing backup functionality.
- **XML theme** - Updated parent theme to Theme.AppCompat.Light.NoActionBar for AppCompatActivity compatibility.

### Removed
- **Unused color utilities** - Removed ColorFamily data class and unspecified_scheme from Theme.kt (were generated scaffolding, unused).

---

- **Backup & Restore** - Export all tasks (including archived) to JSON or CSV files via Android Storage Access Framework.
- **Import backup** - Restore tasks from a JSON backup file with validation and confirmation dialog.
- **Settings screen** - New settings screen accessible from the main task list top bar with a gear icon.
- **CSV export** - Export tasks as RFC 4180 compliant CSV spreadsheet for use in external tools.
- **Backup validation** - Imported tasks are sanitized (blank titles skipped, priority clamped, timestamps corrected).
- **kotlinx-serialization** - Added kotlinx-serialization-json dependency for structured JSON backup format.
- **ProGuard rules** - Added rules for kotlinx-serialization to ensure release builds work correctly.
- **Unit tests** - Added tests for JsonBackupSerializer, CsvBackupSerializer, and TaskBackupValidator.
