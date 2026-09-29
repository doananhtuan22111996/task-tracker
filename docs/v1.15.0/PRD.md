# [TaskTracker] PRD — v1.15.0: Inline Expandable Subtasks & Quick Capture

**Status**: Approved  
**Version**: 1.15.0  
**Created**: 2026-09-27  
**Author**: Solo Indie Developer  
**Parent Scope**: [[TaskTracker] Scope — v1.15.0: Release Candidates & Roadmap](https://app.notion.com/p/3e8b17725413810e8da5c4eedc424545)

---

## 1. Overview & Problem Statement

Task Tracker `v1.14.0` successfully delivered tablet landscape split-pane calendar support, single-task archive undo, and widget documentation. However, daily checklist usability and external capture present friction:

1. **Checklist Execution Friction (ST-30)**:
   - Tasks with subtasks display an aggregated progress indicator (`2/5`) on `TaskItem`.
   - Checking off or viewing subtasks requires navigating away into the full `TaskEditorScreen`. For checklist workflows (grocery shopping, travel packing, multi-step chores), leaving the list to check off an item and returning back creates high interaction fatigue.
   - Users need to expand subtasks directly on the task list and calendar day agenda and check them off in place.

2. **External Task Capture Friction (CAP-01)**:
   - When browsing the web in Chrome or reading articles, saving a link or snippet to Task Tracker requires manual copying, switching apps, tapping `+`, and pasting into fields.
   - Android's native Share Sheet target enables 1-tap capture from any app.

---

## 2. Goals & Success Metrics

- **1-Tap Inline Subtask Toggle**: Users can expand any task card with subtasks directly on `TaskListScreen` and `CalendarScreen` (both bottom sheet and tablet split-pane) to view and check off subtasks without opening `TaskEditorScreen`.
- **Zero Database Migrations**: Full reuse of existing Room v12 `subtasks` schema (`id`, `taskId`, `title`, `isCompleted`, `sortOrder`).
- **Responsive Animations & Performance**: Smooth `animateContentSize` expand/collapse transitions without layout jank in Compose `LazyColumn` (60 fps).
- **Share Target Integration**: Android `ACTION_SEND` intent filter captures shared text and URLs into task creation seamlessly.
- **Accessibility & i18n**: 100% TalkBack semantic coverage on expand/collapse and subtask toggles; full string localization across all 8 supported languages (`en`, `de`, `es`, `fr`, `hi`, `in`, `pt`, `vi`).
- **Stability**: Maintain crash-free rate $\ge 99.5\%$, 100% JVM test coverage on all new business logic.

---

## 3. User Stories

- **US-01 (Checklist in List)**: As a user reviewing my tasks on `TaskListScreen`, I want to tap an expand icon on tasks with subtasks to view the subtask checklist right on the card, and tap checkboxes to mark subtasks done without opening the editor.
- **US-02 (Checklist in Calendar)**: As a user checking my day agenda on `CalendarScreen` (or tablet split-pane), I want to expand and toggle subtasks directly so I can complete multi-step tasks from my schedule.
- **US-03 (Quick Add Subtask)**: As a user viewing an expanded task, I want a quick single-line input field to append a subtask directly without opening the full editor.
- **US-04 (Share to App)**: As a user browsing a website or message, I want to tap "Share" and select "Task Tracker" to immediately create a task with the URL or text prefilled.
- **US-05 (TalkBack Navigation)**: As a visually impaired user, I want TalkBack to clearly announce whether a task card is expanded or collapsed, announce subtask check states, and provide accessible actions.

---

## 4. Functional Requirements

### 4.1 Data & Domain Layer (ST-31)
- **FR-01**: `SubtaskDao` SHALL provide `observeAllSubtasks(): Flow<List<Subtask>>` ordered by `taskId ASC, sortOrder ASC, id ASC`.
- **FR-02**: `ISubtaskRepository` and `SubtaskRepository` SHALL expose `observeSubtasksGroupedByTaskId(): Flow<Map<Long, List<Subtask>>>`.
- **FR-03**: `SubtaskUseCase` SHALL expose `observeSubtasksByTaskId(): Flow<Map<Long, List<Subtask>>>` returning active subtasks grouped by `taskId`.
- **FR-04**: `SubtaskUseCase.setCompleted(subtaskId, completed)` and `SubtaskUseCase.addSubtask(taskId, title)` SHALL remain the single entry points for mutations, preserving existing analytics (`subtask_added`).

### 4.2 UI Component Layer (ST-32)
- **FR-05**: When a task has at least one subtask (`subtaskProgress != null && total > 0`), `TaskItem` SHALL display an expand/collapse button (chevron icon) adjacent to the subtask progress bar.
- **FR-06**: `TaskItem` SHALL support an `isExpanded: Boolean` state parameter and `onToggleExpand: () -> Unit` callback.
- **FR-07**: When expanded, `TaskItem` SHALL render `InlineSubtaskList` composable beneath the task description/tags with smooth `animateContentSize`.
- **FR-08**: `InlineSubtaskList` SHALL render each subtask with:
  - Material 3 `Checkbox` bound to `subtask.isCompleted`.
  - Subtask title with `LineThrough` text decoration and dimmed alpha when completed.
  - Dedicated TalkBack state descriptions ("checked", "not checked", "subtask [title]").
- **FR-09**: When expanded, `InlineSubtaskList` SHALL include a compact inline "Add subtask..." text field with keyboard `Done` action calling `onAddSubtask(title)`.

### 4.3 Screen & ViewModel Integration (ST-33, ST-34)
- **FR-10**: `TaskViewModel` SHALL maintain `expandedTaskIds: StateFlow<Set<Long>>` surviving configuration changes, with `toggleTaskExpanded(taskId: Long)` and `onToggleSubtaskComplete(subtaskId: Long, completed: Boolean)`.
- **FR-11**: `CalendarViewModel` SHALL maintain equivalent `expandedTaskIds` and event handlers for the calendar day agenda.
- **FR-12**: `TaskListScreen` and `DayAgendaContent` SHALL pass expanded states and event handlers down to `TaskItem`.

### 4.4 OS Quick Capture Share Target (CAP-01)
- **FR-13**: `AndroidManifest.xml` SHALL register an intent-filter on `MainActivity` for `android.intent.action.SEND` with mimeType `text/plain`.
- **FR-14**: `MainActivity` SHALL handle incoming `ACTION_SEND` intents, extracting `Intent.EXTRA_TEXT` and `Intent.EXTRA_SUBJECT`.
- **FR-15**: If the shared text contains a URL, the URL SHALL be placed in description and subject/title in title; otherwise text is set as title. Routes to `task_editor` with prefilled state.

---

## 5. Non-Functional Requirements

- **NFR-01 (Zero Schema Migration)**: Room database version remains 12. No migrations required.
- **NFR-02 (Performance)**: LazyColumn rendering of expandable cards SHALL avoid unnecessary recompositions via `remember` and stable lambdas.
- **NFR-03 (Offline-First)**: 100% offline operation. All subtask mutations persist directly to Room.
- **NFR-04 (Accessibility)**: TalkBack content descriptions and state descriptions on expand buttons and inline checkboxes.
- **NFR-05 (i18n)**: All new UI strings localized across all 8 supported languages (`en`, `de`, `es`, `fr`, `hi`, `in`, `pt`, `vi`).
