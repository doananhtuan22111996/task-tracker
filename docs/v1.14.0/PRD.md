# [TaskTracker] PRD — v1.14.0: Tablet Split-Pane + Calendar Polish & FAQ

**Status**: Approved  
**Version**: 1.14.0  
**Created**: 2026-09-20  
**Author**: Solo Indie Developer  
**Parent Roadmap**: Option A (Post-v1.13.0 carry-overs & polish)

---

## 1. Overview & Problem Statement

With `v1.13.0` released to production, core features including Widget v2 and Calendar Day Agenda Multi-Select (CAL-20) are live. However, two parked carry-overs and one documentation gap remain:

1. **Large Screen / Tablet Experience (CAL-25)**:
   - On tablets (e.g., 10" devices like Pixel Tablet) in landscape mode (`sw600dp`), the Calendar screen renders a full-width month grid, leaving wide empty space, and forces day agenda details into a `ModalBottomSheet` that obscures the calendar.
   - Users cannot view the month context and day agenda side-by-side.

2. **Single-Task Archive Undo on Calendar Agenda (CAL-21)**:
   - While batch archive in multi-select mode (CAL-20) provides an atomic Undo snackbar, single-task archive from the day agenda bottom sheet has no Undo affordance, risking accidental archiving without immediate recovery.
   - Previously parked due to observability and layering challenges on modal sheets, now unblocked by `v1.12.0` breadcrumb instrumentation (`FB-12`).

3. **Help & FAQ Documentation (V13-26)**:
   - Widget v2 (resizing, configuring sources, completing from home screen) and Calendar agenda multi-select have shipped without dedicated entries in `HelpScreen`.

---

## 2. Goals & Success Metrics

- **CAL-25 Tablet Ergonomics**: On expanded/tablet landscape viewports (`WindowWidthSizeClass.Expanded` or `sw600dp` landscape), display a two-pane layout: month grid on the left (50%), inline day agenda on the right (50%). No bottom sheet is opened.
- **CAL-21 Single-Task Undo**: Single-task archive from the calendar agenda presents a Snackbar with an "Undo" action for 5 seconds. Tapping Undo restores the task (`isArchived = false`, `archivedAt = null`).
- **V13-26 Help Coverage**: Add a dedicated `WIDGETS` FAQ section and update `CALENDAR` FAQ to cover multi-select and tablet navigation across all 8 supported locales.
- **Zero Regressions**: Maintain crash-free rate $\ge 99.5\%$, ANR-free rate $\ge 99.8\%$, zero database migrations.

---

## 3. User Stories

- **US-01 (CAL-25)**: As a tablet user holding my device in landscape, I want the Calendar tab to display the month grid on the left and selected day agenda on the right simultaneously, so I can plan my schedule without opening and closing modals.
- **US-02 (CAL-25)**: As a tablet user, when I rotate my tablet to portrait, I want the standard phone layout (month grid + bottom sheet) preserved so vertical space is prioritized.
- **US-03 (CAL-25)**: As a tablet user browsing months, when I swipe to a new month, I want my selected day preserved if it exists in the visible month, or an informative placeholder shown if it falls outside.
- **US-04 (CAL-21)**: As a user reviewing my day agenda, if I archive a task by mistake, I want an Undo snackbar to appear immediately so I can restore it in one tap without navigating to the Archived tab.
- **US-05 (V13-26)**: As a user exploring the new widgets or multi-select features, I want clear answers in the Help & FAQ screen explaining how to configure widgets and select multiple tasks.

---

## 4. Functional Requirements

### 4.1 Tablet Split-Pane (CAL-25)
- **FR-01**: The app SHALL detect window size classes using `WindowWidthSizeClass` (via `androidx.compose.material3.windowsizeclass` or `androidx.compose.material3.adaptive`).
- **FR-02**: When window width is `Expanded` (or `Medium` in landscape orientation on `sw600dp+`), `CalendarScreen` SHALL render a two-pane horizontal layout with 50/50 width distribution.
- **FR-03**: The left pane SHALL host `CalendarMonthView` and month navigation top bar.
- **FR-04**: The right pane SHALL host `DayAgendaContent` (extracted from `DayAgendaSheet`) directly inline without a `ModalBottomSheet` wrapper or modal barrier.
- **FR-05**: In compact/portrait configurations, `CalendarScreen` SHALL continue to use `DayAgendaSheet` (modal bottom sheet) upon day selection.
- **FR-06**: When swiping months on the tablet two-pane view, if `selectedDay` falls within the visible month, it remains selected. If `selectedDay` falls outside, the right pane displays an empty/selection state prompt ("Select a day to view agenda").

### 4.2 Agenda Archive Undo (CAL-21)
- **FR-07**: Archiving a single task from the agenda (both modal and inline tablet split-pane) SHALL emit a `UiEvent.ShowUndoDelete` / `UiEvent.ShowSnackbar` with action label "Undo" (duration 5s).
- **FR-08**: Tapping "Undo" SHALL invoke `taskManager.unarchiveTask(taskId)` and restore the task into the agenda view.
- **FR-09**: The Snackbar SHALL be clearly visible regardless of whether the modal bottom sheet is open or if the tablet split-pane is active.

### 4.3 Help & FAQ (V13-26)
- **FR-10**: `HelpFaqSection` enum SHALL include `WIDGETS`.
- **FR-11**: `HelpScreen` SHALL include questions and answers for:
  - Adding and resizing widgets (2x2, 4x2, 4x4)
  - Selecting widget content sources (Today, Upcoming 7 days, Pinned, Tag)
  - Completing tasks directly from the home screen
  - Using long-press multi-select on the day agenda
- **FR-12**: All new FAQ strings SHALL be localized across the 8 supported languages (`en`, `de`, `es`, `fr`, `hi`, `in`, `pt`, `vi`).

---

## 5. Non-Functional Requirements

- **NFR-01 (Clean Architecture)**: Domain business rules (`TaskManager`, `CalendarUseCase`) remain free of Android UI and WindowSize dependencies.
- **NFR-02 (Performance)**: Tablet split-pane recomposition on month swipe must maintain 60 fps without layout jank.
- **NFR-03 (Offline-First)**: All functionality operates 100% offline. No network requests.
- **NFR-04 (Accessibility)**: TalkBack navigation for two-pane mode must define clear landmark semantics for "Month Calendar" and "Day Agenda", and announce pane updates politely.
- **NFR-05 (Build & Size)**: No Room database migrations required. APK size delta $\le 150$ KB.
