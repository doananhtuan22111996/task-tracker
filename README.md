# Task Tracker 📝

A modern, offline-first task tracking Android app built with **Kotlin**, **Jetpack Compose**, and **Material 3**. Designed for personal productivity with a focus on clean architecture, performance, and user experience.

## ✨ Features

### Core Functionality
- ✅ **CRUD Operations** - Create, read, update, and delete tasks
- 🔍 **Smart Search** - Real-time search across task titles and descriptions with debounce
- 🏷️ **Status Filtering** - Filter tasks by status (All, Active, Completed)
- 📊 **Advanced Sorting** - Multiple sorting options with completion grouping
- 💾 **Offline First** - Works completely offline with Room database
- 🎨 **Material 3 Design** - Modern UI following Material Design guidelines

### Sorting & Organization
- **Sort by Creation Date**: Newest first (default) or oldest first
- **Sort by Title**: Alphabetical (A-Z) with locale-safe, case-insensitive ordering
- **Completion Grouping**: Option to group completed tasks separately
- **Stable Sorting**: Deterministic ordering with secondary sort keys

### User Experience
- 🚀 **Instant Feedback** - Real-time updates and responsive UI
- 📱 **Intuitive Interface** - Clean, distraction-free design
- ⚡ **Fast Performance** - Optimized StateFlow combinations and efficient rendering
- 🔄 **State Persistence** - Maintains search/filter state across app sessions

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
│   └── database/
│       ├── Task.kt                 # Task entity
│       ├── TaskDao.kt              # Room data access object
│       └── TaskDatabase.kt         # Room database configuration
├── domain/
│   ├── model/                      # Domain models and data classes
│   │   ├── TaskSort.kt            # Sorting enums and configuration
│   │   └── ...
│   ├── service/                    # Pure business logic services
│   │   └── TaskSortService.kt     # Sorting algorithms and rules
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
│   │   └── TaskListScreen.kt      # Primary task list screen
│   ├── state/                     # State management
│   │   ├── TaskListStateManager.kt   # List state coordination
│   │   └── TaskFormStateManager.kt   # Form state management
│   ├── manager/                   # Business logic coordinators
│   │   └── TaskCrudManager.kt     # CRUD operations coordination
│   └── viewmodel/
│       ├── TaskViewModel.kt       # Main ViewModel coordinator
│       └── TaskFilter.kt          # Filter enum
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
2. **Edit Task** - Tap on any task to edit its title and description
3. **Complete Task** - Tap the checkbox to mark a task as complete/incomplete
4. **Delete Task** - Use the delete button in the task item

### Advanced Features
1. **Search Tasks** - Type in the search field to find tasks by title or description
2. **Filter by Status** - Use the tabs (All, Active, Completed) to filter tasks
3. **Sort Tasks** - Tap the sort button (⚙️) to choose sorting options:
   - Created: Newest first (default)
   - Created: Oldest first
   - Title: A-Z
4. **Group Completed** - Toggle "Completed last" to group completed tasks at the bottom

### Pro Tips
- 🔍 **Search is live** - Results update as you type with smart debouncing
- ⚡ **Instant sorting** - Changes apply immediately when you select options
- 💾 **State persistence** - Your search and filter settings are remembered
- 🎯 **Efficient UI** - Optimized for performance with large task lists

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
