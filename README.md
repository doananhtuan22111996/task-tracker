# Task Tracker 📝

A modern, offline-first task tracking Android app built with **Kotlin**, **Jetpack Compose**, and **Material 3**. Designed for personal productivity with a focus on clean architecture, performance, and user experience.

## ✨ Features

### Core Functionality
- ✅ **CRUD Operations** - Create, read, update, and archive tasks with confirmation
- 🗄️ **Archive System** - Safe task archiving (soft delete) with confirmation dialog and undo functionality
- 🔄 **Archive Management** - Dedicated archived tasks screen with restore and permanent delete options
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
│   └── scheduler/
│       └── WorkManagerTaskReminderScheduler.kt # WorkManager reminder implementation
├── domain/
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
│   │   └── ArchivedScreen.kt      # Archived tasks management screen
│   ├── state/                     # State management
│   │   ├── TaskListStateManager.kt   # List state coordination
│   │   └── TaskFormStateManager.kt   # Form state management
│   ├── manager/                   # Business logic coordinators
│   │   └── TaskCrudManager.kt     # CRUD operations coordination
│   └── viewmodel/
│       ├── TaskViewModel.kt       # Main ViewModel coordinator
│       └── TaskFilter.kt          # Filter enum
├── utils/
│   └── DateUtils.kt              # Date formatting and overdue detection
├── work/
│   └── TaskReminderWorker.kt     # WorkManager worker for notifications
├── TaskTrackerApplication.kt     # Application class with notification setup
└── MainActivity.kt                # App entry point
```

## 🚀 Getting Started

### Prerequisites
- **Android Studio Hedgehog** (2023.1.1) or later
- **JDK 17** or later
- **Android SDK 34** (compileSdk)
- **Minimum SDK 24** (Android 7.0)

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

## 📱 Usage

### Basic Operations
1. **Add Task** - Tap the floating action button (➕) to create a new task
2. **Edit Task** - Tap on any task to edit its title and description (in normal mode)
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
```kotlin
// Unit Tests (Fast)
TaskSortServiceTest           // Pure sorting algorithm tests
TaskFormStateManagerTest      // Form validation tests

// Integration Tests (Medium)
TaskListStateManagerTest      // StateFlow integration tests
TaskCrudManagerTest          // Use case coordination tests

// UI Tests (Comprehensive)
TaskViewModelTest            // End-to-end coordination tests
```

## 🎯 Performance Optimizations

- **StateFlow Optimization** - Efficient combination of multiple reactive streams
- **Lazy Composition** - UI components render only when needed
- **Stable Sorting** - Deterministic ordering prevents unnecessary recompositions
- **Debounced Search** - Smart input handling reduces database queries
- **Key-based LazyColumn** - Optimized list rendering with proper item keys

## 📚 Recent Updates

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

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow **MVVM + Clean Architecture** patterns
- Maintain **single responsibility** for each component
- Write **unit tests** for business logic
- Use **StateFlow** for reactive state management
- Follow **Material 3** design guidelines

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
