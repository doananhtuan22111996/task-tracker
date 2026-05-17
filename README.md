# Task Tracker 📝

A modern, offline-first task tracking Android app built with **Kotlin**, **Jetpack Compose**, and **Material 3**. Designed for personal productivity with a focus on clean architecture, performance, and user experience.

## ✨ Features

### Core Functionality
- ✅ **CRUD Operations** - Create, read, update, and archive tasks with confirmation
- 🗄️ **Archive System** - Safe task archiving (soft delete) with confirmation dialog and undo functionality
- 🔄 **Archive Management** - Dedicated archived tasks screen with restore and permanent delete options
- 📊 **Minimal Stats** - Lightweight statistics view showing active, completed, completed today, due today, and overdue task counts
- 🎯 **Multi-Select & Bulk Actions** - Select multiple tasks for bulk archive, mark completed, or mark active
- 🔍 **Smart Search** - Real-time search across task titles and descriptions with debounce
- 🏷️ **Status Filtering** - Filter tasks by status (All, Active, Completed)
- 🏷️ **Tags/Labels** - Single optional tag per task stored locally for organization and filtering
- 📌 **Pin Tasks** - Pin important tasks to keep them at the top within each day section
- 🎯 **Priority Levels** - Assign Low, Medium, or High priority to tasks with optional priority-based sorting
- 📊 **Advanced Sorting** - Multiple sorting options including priority (High to Low) with completion grouping
- 📅 **Day-based Grouping** - Tasks automatically grouped by day (Today/Yesterday/Date) in all tabs for better readability
- 📅 **Future-Only Due Dates** - Set optional due dates and times (future dates only) with visual overdue indicators
- ⏰ **Validated Local Reminders** - WorkManager-powered notifications (1 minute, 5 minutes, 1 hour, or 1 day before) with automatic validation ensuring reminder time is in the future
- 📝 **Production-Ready Validation** - Comprehensive form validation with required title, input trimming, length limits, and disabled save when no changes
- 💾 **Offline First** - Works completely offline with Room database
- 🎨 **Material 3 Design** - Modern UI following Material Design guidelines
- 🌍 **Multi-Language Support** - Per-app language selection with system language follow option. Ships with 8 languages: English, Vietnamese, German, Spanish, French, Hindi, Indonesian, and Portuguese.
- 🎨 **Theme System** - Light, Dark, and System default theme modes with dynamic color (Material You) on Android 12+
- ⚙️ **Settings** - Dedicated settings screen for appearance, language, and backup preferences with DataStore persistence

### Backup & Restore
- **Export as JSON** - Save all tasks (including archived) to a JSON backup file via the system file picker
- **Import from JSON** - Restore tasks from a JSON backup file with confirmation dialog and validation
- **Export as CSV** - Save all tasks as an RFC 4180 compliant CSV spreadsheet for use in external tools
- **Settings Screen** - Dedicated settings screen accessible from the gear icon in the top bar
- **Backup Validation** - Imported data is sanitized: blank titles skipped, priority clamped, timestamps corrected

### Sorting & Organization
- **Sort by Creation Date**: Newest first (default) or oldest first
- **Sort by Title**: Alphabetical (A-Z) with locale-safe, case-insensitive ordering
- **Sort by Priority**: High to Low priority ordering
- **Pin First**: Pinned tasks always appear first within each day section
- **Completion Grouping**: Option to group completed tasks separately
- **Stable Sorting**: Deterministic ordering with secondary sort keys

### User Experience
- 🚀 **Instant Feedback** - Real-time updates and responsive UI
- 📱 **Intuitive Interface** - Clean, distraction-free design
- ⚡ **Fast Performance** - Optimized StateFlow combinations and efficient rendering
- 🔄 **State Persistence** - Maintains search/filter state across app sessions
- 🛡️ **Safe Deletion** - Confirmation dialogs with undo capability prevent accidental data loss
- 👆 **Long-press Selection** - Long-press to enter selection mode, tap to toggle selection
- ✨ **Polished Form UX** - Auto-focus input, IME Done action, character counters, and data safety with input trimming
- 📱 **Dedicated Task Editor Screen** - Full-screen task editor optimized for small devices with better keyboard handling, scrolling, and field layout
- 🧭 **Bottom navigation for All/Active/Completed** - Modern navigation pattern for task status filtering
- 📝 **Modern Material 3 list items for improved readability** - Streamlined task display with clear visual hierarchy
- 📋 **Task list redesigned to Material 3 modern list items and compact section headers** - Enhanced visual design following Material 3 guidelines from Figma kit
- 🏷️ **Task tags are displayed at the bottom of each task item for improved readability** - Modern chip layout following Material 3 design principles

## 🏗️ Architecture

### Clean Architecture with MVVM
The app follows **Clean Architecture** principles with **MVVM** pattern and **reactive programming**:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Presentation  │    │     Domain       │    │      Data       │
│                 │    │                  │    │                 │
│ • UI Components │◄──►│ • Use Cases      │◄──►│ • Repository    │
│ • ViewModels    │    │ • Business Logic │    │ • Room Database │
│ • State Managers│    │ • Services       │    │ • DAOs          │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Modular Design
Recent refactoring has separated concerns into focused, maintainable components:

#### ViewModel Layer (Thin Coordinators)
- **TaskViewModel** - Coordinates UI state and user actions
- Delegates business logic to specialized managers
- Exposes reactive state flows to UI

#### State Management Layer
- **TaskListStateManager** - Manages list state, search, filter, and sorting
- **TaskFormStateManager** - Handles form validation and dialog state
- **TaskCrudManager** - Coordinates CRUD operations with error handling

#### Business Logic Layer
- **TaskSortService** - Pure sorting algorithms and business rules
- **Use Cases** - Specialized operations (CRUD, Search, Filter, Form)
- **Repository Pattern** - Data access abstraction

#### UI Layer (Modular Components)
- **TaskListScreen** - Main coordinator screen
- **TaskListTopBar** - Top app bar with sort functionality
- **SortMenu** - Sort options dropdown with radio buttons
- **TaskListContent** - Main content area with search, filter, and task list

## 🛠️ Technology Stack

### Core Technologies
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern declarative UI toolkit
- **Material 3** - Latest Material Design components and theming

### Architecture Components
- **ViewModel** - UI-related data holder with lifecycle awareness
- **StateFlow** - Reactive state management with coroutines
- **Room Database** - Local SQLite database with compile-time verification
- **WorkManager** - Background task scheduling for reminder notifications
- **Hilt** - Dependency injection for Android
- **DataStore** - Persistent key-value preferences for theme, language, and settings
- **AppCompat** - Per-app language support with backward compatibility (API 26+)

### Data & Persistence
- **Room** - Offline-first local database
- **Flow** - Reactive data streams for real-time updates
- **Coroutines** - Asynchronous programming with structured concurrency

### Development Tools
- **Gradle** - Build automation and dependency management
- **KSP** - Kotlin Symbol Processing for Room and Hilt
- **ProGuard** - Code shrinking and obfuscation for release builds

## 📁 Project Structure

```
app/src/main/java/dev/tuandoan/tasktracker/
├── data/
│   ├── database/
│   │   ├── Task.kt                 # Task entity with due dates and reminders
│   │   ├── TaskDao.kt              # Room data access object
│   │   └── TaskDatabase.kt         # Room database with migration support
│   ├── backup/
│   │   ├── dto/
│   │   │   ├── TaskBackupDto.kt         # Serializable DTO for task backup
│   │   │   └── BackupPayload.kt         # JSON envelope with metadata
│   │   ├── BackupSerializer.kt          # Serialization interface
│   │   ├── JsonBackupSerializer.kt      # JSON implementation (kotlinx.serialization)
│   │   ├── CsvBackupSerializer.kt       # CSV implementation (RFC 4180)
│   │   ├── BackupFileProvider.kt        # File I/O interface
│   │   └── AndroidBackupFileProvider.kt # ContentResolver implementation
│   ├── scheduler/
│   │   └── WorkManagerTaskReminderScheduler.kt # WorkManager reminder implementation
│   └── preferences/
│       ├── SettingsRepository.kt        # DataStore-backed settings persistence
│       └── UserPreferences.kt           # Data class for theme, language, dynamic color prefs
├── di/
│   └── PreferencesModule.kt             # Hilt module for DataStore and settings DI
├── domain/
│   ├── backup/
│   │   ├── model/
│   │   │   ├── BackupFormat.kt        # Enum (JSON, CSV)
│   │   │   ├── BackupMetadata.kt      # Metadata with schema version
│   │   │   ├── ExportResult.kt        # Sealed class for export results
│   │   │   └── ImportResult.kt        # Sealed class for import results
│   │   ├── BackupValidator.kt         # Validation interface
│   │   ├── TaskBackupValidator.kt     # Validation rules implementation
│   │   ├── ExportBackupUseCase.kt     # Export orchestration
│   │   └── ImportBackupUseCase.kt     # Import orchestration
│   ├── model/                      # Domain models and data classes
│   │   ├── TaskSort.kt            # Sorting enums and configuration
│   │   ├── ReminderOption.kt      # Reminder time options enum
│   │   └── ...
│   ├── service/                    # Pure business logic services
│   │   └── TaskSortService.kt     # Sorting algorithms and rules
│   ├── scheduler/                  # Background task scheduling
│   │   └── TaskReminderScheduler.kt # Reminder scheduling interface
│   └── usecase/                   # Business use cases
│       ├── TaskCrudUseCase.kt     # CRUD operations
│       ├── TaskSearchUseCase.kt   # Search with debounce
│       ├── TaskFilterUseCase.kt   # Status filtering
│       └── TaskFormUseCase.kt     # Form management
├── ui/
│   ├── components/                 # Reusable UI components
│   │   ├── TaskListTopBar.kt      # Top bar with sort menu
│   │   ├── SortMenu.kt            # Sort dropdown component
│   │   ├── TaskListContent.kt     # Main content area
│   │   ├── TaskItem.kt            # Individual task display
│   │   ├── SearchField.kt         # Search input component
│   │   └── FilterTabs.kt          # Status filter tabs
│   ├── screens/                   # Main screens
│   │   ├── TaskListScreen.kt      # Primary task list screen
│   │   ├── TaskEditorScreen.kt    # Dedicated full-screen task editor (optimized for small devices)
│   │   └── ArchivedScreen.kt      # Archived tasks management screen
│   ├── state/                     # State management
│   │   ├── TaskListStateManager.kt   # List state coordination
│   │   └── TaskFormStateManager.kt   # Form state management
│   ├── manager/                   # Business logic coordinators
│   │   └── TaskCrudManager.kt     # CRUD operations coordination
│   └── viewmodel/
│       ├── TaskViewModel.kt       # Main ViewModel coordinator
│       ├── TaskEditorViewModel.kt # Dedicated ViewModel for task editor screen
│       └── TaskFilter.kt          # Filter enum
├── utils/
│   └── DateUtils.kt              # Date formatting and overdue detection
├── work/
│   └── TaskReminderWorker.kt     # WorkManager worker for notifications
├── TaskTrackerApplication.kt     # Application class with notification setup
└── MainActivity.kt                # App entry point (AppCompatActivity for locale support)

app/src/main/res/
├── values/
│   └── strings.xml                # Default (English) string resources
├── values-de/
│   └── strings.xml                # German (Deutsch) translations
├── values-es/
│   └── strings.xml                # Spanish (Español) translations
├── values-fr/
│   └── strings.xml                # French (Français) translations
├── values-hi/
│   └── strings.xml                # Hindi (हिन्दी) translations
├── values-in/
│   └── strings.xml                # Indonesian (Bahasa Indonesia) translations
├── values-pt/
│   └── strings.xml                # Portuguese (Português) translations
├── values-vi/
│   └── strings.xml                # Vietnamese (Tiếng Việt) translations
└── xml/
    └── locales_config.xml         # Registered locales for per-app language (Android 13+)
```

## 🌍 Internationalization (i18n)

### Supported Languages
- **English** (default)
- **German** (Deutsch)
- **Spanish** (Español)
- **French** (Français)
- **Hindi** (हिन्दी)
- **Indonesian** (Bahasa Indonesia)
- **Portuguese** (Português)
- **Vietnamese** (Tiếng Việt)

### Per-App Language Selection
The app supports per-app language selection on all supported Android versions:
- **Android 13+ (API 33+)**: Native per-app language via `LocaleManager` and system App Languages settings
- **Android 7-12 (API 26-32)**: In-app language override via `AppCompatDelegate.setApplicationLocales()`

Users can change the language in **Settings → Language** without affecting other apps.

### Adding a New Language

To add support for a new locale (e.g., Japanese `ja`):

1. **Create the resource directory:**
   ```bash
   mkdir -p app/src/main/res/values-ja
   ```

2. **Copy and translate `strings.xml`:**
   ```bash
   cp app/src/main/res/values/strings.xml app/src/main/res/values-ja/strings.xml
   ```
   Translate all `<string>` values in the new file. Keep the `name` attributes unchanged.

3. **Register the locale** in `app/src/main/res/xml/locales_config.xml`:
   ```xml
   <locale-config xmlns:android="http://schemas.android.com/apk/res/android">
       <locale android:name="en" />
       <locale android:name="vi" />
       <locale android:name="ja" />  <!-- Add this line -->
   </locale-config>
   ```

4. **Build and test:**
   ```bash
   ./gradlew assembleDebug
   ```
   The new language will automatically appear in the Settings → Language picker.

### Testing Translations
- Change language in **Settings → Language** and verify all screens
- Test with the system language set to the target locale
- Verify date formats, number formats, and text direction
- Check that no English strings appear when using the translated locale
- Test on both API 26 (backward compat) and API 33+ (native)

### String Resource Guidelines
- All user-facing strings are in `app/src/main/res/values/strings.xml`
- Zero hardcoded user-facing strings in Kotlin code
- Use `stringResource(R.string.xxx)` in Compose UI
- Use `context.getString(R.string.xxx)` in non-Compose code
- Parameterized strings use `%s` (string), `%d` (integer) format specifiers

## 🚀 Getting Started

### Prerequisites
- **Android Studio Hedgehog** (2023.1.1) or later
- **JDK 17** or later
- **Android SDK 34** (compileSdk)
- **Minimum SDK 26** (Android 8.0)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/task-tracker.git
   cd task-tracker
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Build and Run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

### Development Setup

1. **Enable Kotlin compiler optimizations**
   ```bash
   # Add to local.properties
   kotlin.compiler.execution.strategy=in-process
   ```

2. **Run tests**
   ```bash
   ./gradlew test                    # Unit tests
   ./gradlew connectedAndroidTest   # Instrumentation tests
   ```

3. **Generate release build**
   ```bash
   ./gradlew assembleRelease
   ```

4. **Crashlytics mapping-file upload** (release builds only — FB-03)
   Release builds upload the R8/ProGuard mapping file to Firebase Crashlytics so the
   console can deobfuscate stack traces. The Crashlytics gradle plugin authenticates
   via the **`GOOGLE_APPLICATION_CREDENTIALS` env var** (the plugin reads it from the
   process environment; Gradle can't bridge env vars to non-Exec tasks).
   ```bash
   # Required: path to a Google service-account JSON key with Crashlytics upload scope.
   export GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/firebase-service-account.json
   ./gradlew :app:bundleRelease
   ```
   If the env var is unset, `bundleRelease` fails fast with a clear message before the
   upload task runs. CI builds are debug-only and never invoke the upload task.

## 📱 Usage

### Basic Operations
1. **Add Task** - Tap the floating action button (➕) to open the full-screen task editor
2. **Edit Task** - Tap on any task to open the dedicated editor screen with all task fields (in normal mode)
3. **Complete Task** - Tap the checkbox to mark a task as complete/incomplete
4. **Archive Task** - Use the archive button to safely archive tasks with confirmation dialog and undo functionality

### Bulk Operations
1. **Enter Selection Mode** - Long-press on any task to enter selection mode
2. **Select Multiple Tasks** - Tap on tasks to toggle selection (highlighted in blue)
3. **Bulk Actions** - Use top bar buttons to:
   - Mark selected tasks as completed (✓)
   - Mark selected tasks as active (□)
   - Archive selected tasks (🗄️) with confirmation and undo
4. **Select All** - Use overflow menu (⋮) to select all visible tasks
5. **Exit Selection** - Tap the close button (✕) to clear selection

### Archive Management
1. **View Archived Tasks** - Navigate to the archived tasks screen to see all archived tasks
2. **Restore Tasks** - Tap restore button on archived tasks to return them to the main task list
3. **Permanent Delete** - Use permanent delete on archived tasks (cannot be undone)
4. **Bulk Archive Operations** - Select multiple archived tasks for bulk restore or permanent deletion

### Advanced Features
1. **Search Tasks** - Type in the search field to find tasks by title or description
2. **Filter by Status** - Use the tabs (All, Active, Completed) to filter tasks
3. **Sort Tasks** - Tap the sort button (⚙️) to choose sorting options:
   - Created: Newest first (default)
   - Created: Oldest first
   - Title: A-Z
   - Priority: High to Low
4. **Group Completed** - Toggle "Completed last" to group completed tasks at the bottom
5. **Set Due Dates** - Tap the calendar icon in Add/Edit dialog to set optional due dates
6. **Configure Reminders** - Choose from 1 minute, 5 minutes, 1 hour, or 1 day before due date (requires future due date and validates reminder time is in the future). On Android 13+, the app will prompt for notification permission when first enabling reminders.
7. **Organize with Tags** - Add an optional single tag to tasks for categorization (up to 20 characters). Tags appear as chips in the task list and can be used for filtering.
8. **Filter by Tags** - Use the tag filter chips below the status tabs to filter tasks by specific tags. Tap a tag chip to filter, tap again to clear.
9. **Pin Important Tasks** - Tap the star button on any task to pin/unpin it. Pinned tasks appear first within each day section.
10. **Set Task Priority** - In the Add/Edit task dialog, use the Priority dropdown to set Low, Medium (default), or High priority levels.
11. **Track Overdue Tasks** - Overdue tasks display in red with "Overdue" indicators in the task list

### Pro Tips
- 🔍 **Search is live** - Results update as you type with smart debouncing
- ⚡ **Instant sorting** - Changes apply immediately when you select options
- 💾 **State persistence** - Your search and filter settings are remembered
- 🎯 **Efficient UI** - Optimized for performance with large task lists
- ⏰ **Smart reminders** - Notifications work offline and survive app restarts via WorkManager
- 📱 **Permission aware** - On Android 13+ (API 33+), notification permission is required for reminders. The app will prompt contextually when enabling reminders.

## 🔧 Development

### Architecture Benefits
- **Testable** - Each component can be unit tested independently
- **Maintainable** - Clear separation of concerns and single responsibilities
- **Scalable** - Modular design allows easy feature additions
- **Performant** - Optimized StateFlow usage and efficient UI rendering

### Code Quality
- **Clean Architecture** - Domain-driven design with clear boundaries
- **SOLID Principles** - Single responsibility, dependency inversion, etc.
- **Reactive Programming** - Flow-based data streams with proper lifecycle handling
- **Material Design** - Consistent UI/UX following platform guidelines

### Testing Strategy

The project has a comprehensive JVM unit test suite covering core business logic, domain services, use cases, state managers, and backup serialization. Tests are fast (< 1 second), deterministic, and require no emulator or device.

#### Running Tests

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run all unit tests (debug + release)
./gradlew test

# Run a specific test class
./gradlew testDebugUnitTest --tests "dev.tuandoan.tasktracker.domain.TaskManagerTest"

# Run with verbose output
./gradlew testDebugUnitTest --info

# Format code before committing
./gradlew spotlessApply
```

#### Test Suite Overview (263 tests)

| Layer | Test Class | Tests | What It Covers |
|-------|-----------|-------|----------------|
| **Domain** | `TaskManagerTest` | 31 | CRUD, completion toggle, archive, bulk ops, reminder scheduling |
| **Domain** | `TaskSortServiceTest` | 16 | Sort by created/title/priority, completed grouping, stability |
| **Domain** | `TaskSortTest` | 10 | Display names, full display names, defaults |
| **Domain** | `ReminderOptionTest` | 10 | fromOffsetMinutes mapping, selectable options, offset values |
| **Domain** | `PriorityTest` | 7 | fromValue mapping, display names, int values |
| **Use Case** | `TaskCrudUseCaseTest` | 23 | Create, update, delete, toggle, restore, bulk ops, archive, pin/priority |
| **Use Case** | `TaskFormUseCaseTest` | 23 | Title/desc/tag validation, length limits, form data trimming, dialog state |
| **Use Case** | `TaskFilterUseCaseTest` | 9 | Status filtering (ALL/ACTIVE/COMPLETED), filter state, hasActiveFilter |
| **Use Case** | `TaskSearchUseCaseTest` | 10 | Case-insensitive search, title+description matching, query state |
| **UI State** | `TaskSelectionStateManagerTest` | 20 | Enter/toggle/clear/selectAll, validation, StateFlow reactivity |
| **UI Manager** | `TaskCrudManagerValidationTest` | 25 | Input validation for bulk ops, archive ops, toggle, pin |
| **UI Manager** | `TaskBulkActionManagerTest` | 20 | Bulk delete/archive/complete/restore flows with confirmation |
| **Backup** | `JsonBackupSerializerTest` | 10 | Round-trip, missing fields, unknown keys, schema version, malformed JSON |
| **Backup** | `CsvBackupSerializerTest` | 13 | Round-trip, escaping, special chars, empty fields, malformed CSV |
| **Backup** | `TaskBackupValidatorTest` | 14 | Blank title skip, priority clamping, timestamp defaults, truncation |
| **Backup** | `TaskBackupDtoTest` | 6 | fromTask/toTask mapping, round-trip, defaults |
| **Utils** | `TaskDateGrouperTest` | 12 | Day grouping, date keys, header labels, pinned ordering, dueAt priority |
| **Utils** | `DateUtilsTest` | 4 | formatDate, formatDueDate, isOverdue |

#### Test Infrastructure

- **Fakes over mocks**: `FakeTaskRepository` and `FakeReminderScheduler` provide deterministic in-memory implementations
- **Test data builder**: `TestTaskFactory` creates consistent test data with fixed timestamps
- **Coroutine testing**: `kotlinx-coroutines-test` with `runTest` for suspend function testing
- **Flow testing**: `Turbine` for testing StateFlow/Flow emissions
- **No Android dependencies**: All tests run on the JVM without Robolectric or emulators
- **No wildcard imports**: Enforced by Spotless/ktlint

#### CI Integration

Add this step to your GitHub Actions workflow:

```yaml
- name: Run unit tests
  run: ./gradlew testDebugUnitTest

- name: Check code formatting
  run: ./gradlew spotlessCheck
```

#### Coverage Gaps (documented, not yet covered)

- **ViewModel tests**: `TaskViewModel`, `TaskEditorViewModel`, `StatsViewModel` (require SavedStateHandle/Hilt mocking)
- **Room migration tests**: Would require instrumentation tests with `MigrationTestHelper`
- **BackupFileProvider**: Android-specific ContentResolver interaction (instrumentation only)
- **WorkManager reminder delivery**: OS-level notification scheduling (integration/E2E only)

## 🎯 Performance Optimizations

- **StateFlow Optimization** - Efficient combination of multiple reactive streams
- **Lazy Composition** - UI components render only when needed
- **Stable Sorting** - Deterministic ordering prevents unnecessary recompositions
- **Debounced Search** - Smart input handling reduces database queries
- **Key-based LazyColumn** - Optimized list rendering with proper item keys

## 📚 Recent Updates

### v3.2 - Localization Expansion (8 Languages)
- 🌍 **6 New Languages** - Added German (Deutsch), Spanish (Español), French (Français), Hindi (हिन्दी), Indonesian (Bahasa Indonesia), and Portuguese (Português) translations
- 🔧 **XML Escaping Fixes** - Corrected apostrophe and special character escaping across all translation files for crash-free string rendering
- 📋 **Locale Registry Update** - Updated `locales_config.xml` to register all 8 supported locales for Android 13+ native language picker
- ⚙️ **Settings Integration** - Language picker in Settings now lists all 8 languages with native display names and scrollable dialog

### v3.1 - Global Language Support & Theme Polish
- 🌍 **Per-App Language Selection** - Choose app language independently from system (English, Vietnamese), with backward compatibility for Android 7-12
- 🎨 **Theme System** - Light/Dark/System mode with DataStore persistence
- ✨ **Dynamic Color (Material You)** - Wallpaper-based color theming on Android 12+ with toggle and fallback
- 🌐 **Full Internationalization** - All user-facing strings extracted to resources with Vietnamese translations
- ⚙️ **Enhanced Settings Screen** - New Appearance (theme, dynamic color) and Language sections
- 📱 **Android 13+ Native Language** - Integrated with system App Languages setting via `locales_config.xml`
- 🔧 **DataStore Preferences** - Theme, dynamic color, and language preferences saved persistently
- 🏗️ **AppCompat Migration** - MainActivity migrated to AppCompatActivity for per-app locale support

### v3.0 - Dedicated Task Editor Screen (Small Device Optimization)
- 📱 **Full-Screen Task Editor** - Replaced modal dialog with dedicated screen optimized for small devices with better usability
- 🔄 **Navigation Refactor** - Implemented Compose Navigation for proper screen-based navigation instead of dialog state management
- ⌨️ **Improved Keyboard Handling** - Full-screen editor with proper IME padding and scrolling to ensure focused fields remain visible
- 📐 **Better Field Layout** - Optimized field spacing, organization sections, and visual hierarchy for small device usability
- 🎯 **Dedicated ViewModel** - New TaskEditorViewModel with focused state management for editor-specific operations
- 🧹 **Code Cleanup** - Removed unused dialog code paths and simplified navigation architecture
- 🏗️ **Architecture Enhancement** - Proper separation between list management (TaskViewModel) and editor functionality (TaskEditorViewModel)
- 📱 **Small Device Focus** - Improved scrolling, field visibility, and touch targets specifically designed for small screens

### v2.9 - UI/UX Enhancements & Material 3 Polish
- 🎨 **Enhanced Typography System** - Comprehensive Material 3 typography hierarchy with proper font weights, sizes, and semantic styles for improved readability and visual hierarchy
- 📐 **Consistent Spacing System** - New 8dp-based spacing system (AppSpacing) ensuring consistent layout throughout the app with specific use-case spacing for cards, buttons, and touch targets
- 🔶 **Custom Shape System** - Refined shape system with custom shapes for task items, chips, dialogs, and other components following Material 3 rounded corner guidelines
- 🏗️ **Enhanced Task Item Design** - Material 3 ListItem hybrid design with improved visual differentiation for completed, overdue, and pinned tasks, enhanced metadata display with chips
- 📱 **Improved Main Screen Layout** - CenterAlignedTopAppBar, enhanced filter row, polished sort UI, refined sticky headers, and better empty state messaging
- ✏️ **Enhanced Add/Edit Task Dialog** - Better form layout with visual grouping (Task Details, Organization), improved field styling, and enhanced action buttons with icons
- 🎯 **Polished Bulk Selection UI** - Clear visual selection states, improved action button styling, and consistent Material 3 theming throughout selection mode
- 🗄️ **Enhanced Archived Screen** - Mirror main screen styling improvements with consistent CenterAlignedTopAppBar and enhanced visual hierarchy
- 📤 **Improved Snackbar Consistency** - Consistent styling and messaging across all operations with proper Material 3 theming
- 🎨 **Global Theme Integration** - All components now use new typography, spacing, and shape systems for cohesive Material 3 design language
- 🧹 **Polished Material 3 Layout** - Optimized spacing and visual hierarchy with single app bar title, removing duplication and excessive whitespace for a more compact, cohesive user interface
- 📱 **Enhanced Archived and Stats Screens** - Polished secondary screens with consistent Material 3 layout, optimized spacing, and improved visual hierarchy matching the main screen design
- 📊 **Single App Bar Architecture** - Archived and Stats screens use a single Material 3 app bar (no duplicated titles) with optimized spacing

### v2.8 - Minimal Stats (Lightweight Analytics)
- 📊 **Stats Screen** - Lightweight statistics view showing key task counts without charts or complex analytics
- 🔢 **Five Key Metrics** - Active tasks, completed tasks (overall), completed today, due today (active only), and overdue counts
- 📅 **Timezone-Aware Today Calculations** - Proper local timezone handling for "today" boundaries using java.time
- 💾 **Completion Timestamps** - Added completedAt field to track when tasks were completed for accurate "completed today" stats
- 🗄️ **Archive Exclusion** - All stats exclude archived tasks by default with clear notation
- 📱 **Material 3 UI** - Clean stats cards accessible from overflow menu in main screen
- 🔄 **Reactive Updates** - Stats update automatically when task data changes using Flow-based architecture
- 💾 **Database Migration** - Seamless Room database upgrade from v5 to v6 with completedAt field
- 🏗️ **Performance Optimized** - Stats computed at database level with efficient queries and proper StateFlow usage
- 📱 **Production-Ready MVP** - Simple numeric stats display without graphs, animations, or third-party dependencies

### v2.7 - Archive System (Soft Delete)
- 🗄️ **Archive Tasks** - Replace permanent delete with safe archiving system using soft delete pattern
- 📱 **Dedicated Archive Screen** - Complete archived tasks management with restore and permanent delete options
- 🔄 **Archive Undo** - Archive operations support undo functionality with appropriate messaging
- 🎯 **Bulk Archive Operations** - Bulk archive, restore, and permanent delete for efficient task management
- ⏰ **Reminder Integration** - Automatic reminder cancellation when tasks are archived, rescheduling when restored
- 💾 **Database Migration** - Seamless Room database upgrade from v4 to v5 with isArchived and archivedAt fields
- 🏗️ **Clean Architecture** - Modular archive system integrated throughout application stack with proper separation of concerns
- 📱 **Production-Ready MVP** - Simple, intuitive archive workflow without advanced animations or complex features
- 🛡️ **Data Safety** - Two-tier deletion system: archive first (recoverable), then permanent delete (final)
- 🎨 **UI Updates** - Archive icons replace delete icons, confirmation dialogs updated with appropriate messaging

### v2.6 - Pin Tasks & Priority Levels
- 📌 **Pin/Unpin Tasks** - Keep important tasks at the top within each day section with star button toggle
- 🎯 **Priority System** - Assign Low, Medium (default), or High priority levels to tasks for better organization
- 📊 **Priority Sorting** - New "Priority: High to Low" sort option to organize tasks by importance
- 💾 **Database Migration** - Seamless Room database upgrade from v3 to v4 with new isPinned and priority fields
- 🎨 **Enhanced UI** - Pin button with filled/outlined star icons and priority dropdown in edit dialog
- 🏗️ **Pinned-First Ordering** - Pinned tasks automatically appear first within each day section while preserving existing sort order
- 📱 **Production-Ready MVP** - Simple, intuitive implementation without advanced animations or extra dependencies
- ✨ **Clean Architecture Integration** - Modular pin/priority system integrated throughout application stack

### v2.5 - Tag Organization & Filtering
- 🏷️ **Single Tag per Task** - Add optional tags to tasks for organization and categorization (up to 20 characters)
- 💾 **Database Migration** - Seamless Room database upgrade from v2 to v3 with new tag field
- 🎨 **Visual Tag Chips** - Tags display as attractive chips in the task list with proper Material 3 styling
- 🔍 **Tag Filtering** - Filter tasks by tags using horizontal scrolling filter chips below status tabs
- ✂️ **Smart Tag Processing** - Automatic trimming and validation with length limits and error feedback
- 🎯 **MVP-Friendly Design** - Simple, single-tag approach without complex tag management or color coding
- 📝 **Form Integration** - Tag input field in Add/Edit task dialog with character counter and validation
- 🏗️ **Clean Architecture** - Modular tag system integrated throughout the application stack

### v2.4 - Due Dates & Local Reminders
- 📅 **Future-Only Due Dates** - Add optional due dates and times to tasks with strict future-only validation
- ⏰ **Validated Smart Reminders** - Local notifications via WorkManager (1 minute, 5 minutes, 1 hour, or 1 day before) with automatic validation ensuring reminder time is in the future
- 🚨 **Overdue Detection** - Visual indicators for overdue tasks with red coloring and "Overdue" labels
- 📱 **Notification System** - Complete notification channel setup with contextual Android 13+ permission requests
- 🔄 **Intelligent Scheduling** - Automatic reminder rescheduling when tasks are edited or completed
- 💾 **Database Migration** - Seamless Room database upgrade from v1 to v2 with new fields
- 🎯 **Enhanced Form Validation** - Comprehensive validation ensuring due dates are future-only and reminder times are valid
- 📅 **Day-based Grouping** - Task list automatically organized by day (Today/Yesterday/specific dates) with sticky headers for improved readability
- 🏗️ **Clean Architecture** - Modular reminder system with WorkManager integration and Hilt DI

> **Note**: On Android 13+ (API 33+), notification permission is required for reminders. The app will prompt contextually when enabling reminders with an education dialog explaining the requirement.

### v2.3 - Production-Ready Validation & Form UX
- 📝 **Comprehensive Form Validation** - Required title validation with real-time error feedback
- ✂️ **Smart Input Processing** - Automatic whitespace trimming on save for clean data
- 📏 **Length Limits** - Title (100 chars) and description (500 chars) limits with character counters
- 🎯 **Auto-Focus & Keyboard UX** - Title field auto-focuses on open, IME Done action for quick save
- 🔒 **Change Detection** - Save button disabled when no changes made in edit mode
- ⚡ **Enhanced Validation Architecture** - Reactive validation with field-specific error states

### v2.2 - Multi-Select & Bulk Actions
- 🎯 **Multi-Select Mode** - Long-press to enter selection mode, tap to toggle task selection
- ⚡ **Bulk Operations** - Efficiently mark multiple tasks as completed/active or delete in batch
- 🔄 **Bulk Delete Safety** - Confirmation dialog and undo functionality for bulk deletions
- 🎨 **Selection UI** - Visual selection indicators and dedicated top bar with bulk action buttons

### v2.1 - Enhanced User Safety
- 🛡️ **Delete Confirmation** - Confirmation dialog prevents accidental task deletion
- 🔄 **Undo Functionality** - 4-second undo window with Material 3 Snackbar integration
- ⚡ **Improved UX** - Safe, user-friendly deletion workflow with instant feedback

### v2.0 - Advanced Sorting & Modular Architecture
- ✨ **Enhanced Sorting** - Multiple sort options with completion grouping
- 🏗️ **Architecture Refactoring** - Separated concerns into focused, testable components
- 🎨 **UI Improvements** - Simplified sort menu with clear radio buttons and toggle
- ⚡ **Performance** - Optimized state management and reduced complexity
- 🧪 **Testing** - Improved testability with modular design

### v1.0 - Core Features
- 📱 **Basic CRUD Operations** - Create, read, update, delete tasks
- 🔍 **Search & Filter** - Real-time search with status filtering
- 💾 **Room Database** - Offline-first local storage
- 🎨 **Material 3 UI** - Modern, clean interface

## 🎯 Code Style & Formatting

This project uses **Spotless** with **ktlint** to enforce consistent code formatting across all Kotlin and Gradle files.

### Running Formatting

```bash
# Apply code formatting fixes
./gradlew spotlessApply

# Check code formatting (fails build if violations exist)
./gradlew spotlessCheck
```

### Configuration

- **Kotlin files**: Formatted with ktlint 1.4.1
- **Gradle files**: Basic formatting (trailing whitespace, newlines)
- **Explicit imports**: Wildcard imports are prohibited - all imports must be explicit for better code clarity and IDE performance
- **Compose-friendly**: Parameter wrapping rules disabled for better Compose readability
- **Line length**: 120 characters maximum
- **Indentation**: 4 spaces

The formatting check runs automatically during the build process. All code must pass formatting checks before merging.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. **Run formatting**: `./gradlew spotlessApply`
4. Commit your changes (`git commit -m 'Add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

### Development Guidelines
- Follow **MVVM + Clean Architecture** patterns
- Maintain **single responsibility** for each component
- Write **unit tests** for business logic
- Use **StateFlow** for reactive state management
- Follow **Material 3** design guidelines
- **Code formatting**: Run `./gradlew spotlessApply` before committing

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Material Design** - UI/UX design system
- **Jetpack Compose** - Modern Android UI toolkit
- **Android Architecture Components** - Lifecycle-aware components
- **Room Database** - SQLite abstraction layer
- **Hilt** - Dependency injection framework

---

**Built with ❤️ for productivity and clean code**
