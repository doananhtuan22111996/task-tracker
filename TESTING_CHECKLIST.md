# Task Editor Screen - Manual Testing Checklist

This checklist covers comprehensive testing of the new dedicated Task Editor screen functionality, focusing on small device usability and regression prevention.

## 🎯 Core Functionality Tests

### ✅ Task Creation
- [ ] **FAB Navigation**: Tap FAB on main screen → opens TaskEditorScreen in create mode
- [ ] **Screen Title**: Editor shows "New task" in top bar
- [ ] **Auto-focus**: Title field automatically receives focus on screen open
- [ ] **Empty Form**: All fields start empty with appropriate defaults (Priority = Medium, Pin = false)
- [ ] **Save State**: Save button is initially disabled (no changes)

### ✅ Task Editing
- [ ] **Navigation**: Tap any task in list → opens TaskEditorScreen in edit mode
- [ ] **Screen Title**: Editor shows "Edit task" in top bar
- [ ] **Field Population**: All fields populate correctly with existing task data
- [ ] **Save State**: Save button is initially disabled (no changes detected)
- [ ] **Change Detection**: Save button enables only after making actual changes

### ✅ Save Functionality
- [ ] **Title Required**: Cannot save without title, shows inline error
- [ ] **Title Trimming**: Leading/trailing whitespace is trimmed on save
- [ ] **Save Success**: Save completes successfully and navigates back to list
- [ ] **List Update**: Created/edited task appears correctly in main list
- [ ] **Keyboard Dismiss**: Keyboard dismisses on successful save

### ✅ Navigation & Back Handling
- [ ] **Back Button**: Top bar back button navigates to main screen
- [ ] **Android Back**: System back gesture/button works correctly
- [ ] **No Duplicates**: No duplicate screens in navigation stack
- [ ] **State Management**: Navigation preserves main screen state (search, filters)

## 📱 Small Device Optimization Tests

### ⌨️ Keyboard Handling
- [ ] **Field Visibility**: Focused field remains visible when keyboard opens
- [ ] **IME Padding**: Content adjusts properly with keyboard (no overlap)
- [ ] **Scrolling**: Can scroll to access all fields when keyboard is open
- [ ] **IME Actions**: Title field IME "Done" action saves task if valid

### 📐 Layout & Spacing
- [ ] **Field Spacing**: Adequate spacing (8-12dp) between form fields
- [ ] **Touch Targets**: All buttons and fields have proper touch target size (48dp min)
- [ ] **Content Padding**: 16dp horizontal padding throughout
- [ ] **Scrollable**: Full content scrolls properly on small screens
- [ ] **No Clipping**: No UI elements are cut off or inaccessible

### 🎯 Field Organization
- [ ] **Field Order**: Title → Description → Organization → Due Date → Reminder → Pin
- [ ] **Visual Grouping**: Clear sections with appropriate headers
- [ ] **Priority Dropdown**: Works correctly with all three levels
- [ ] **Date Picker**: Date selection dialog functions properly

## 🔍 Validation & Error Handling Tests

### ✅ Form Validation
- [ ] **Empty Title**: Shows error message when title is empty
- [ ] **Title Length**: Enforces max length (100 chars) with character counter
- [ ] **Description Length**: Enforces max length (500 chars) with character counter
- [ ] **Tag Length**: Enforces max length (20 chars) with validation
- [ ] **Inline Errors**: Error messages appear below relevant fields

### 📅 Date & Reminder Validation
- [ ] **Future Dates**: Due date must be in future, shows error for past dates
- [ ] **Reminder Dependency**: Reminder only enabled when due date is set
- [ ] **Reminder Validation**: Reminder time must be in future (due date - offset > now)
- [ ] **Error Display**: Clear error messages for invalid date/reminder combinations

### 🔔 Permission Handling (Android 13+)
- [ ] **Permission Check**: Prompts for notification permission when enabling reminders
- [ ] **Permission Education**: Shows clear explanation of why permission is needed
- [ ] **Graceful Fallback**: Can save task without permission (reminder won't fire)

## 🎨 UI/UX Polish Tests

### 🎯 Material 3 Compliance
- [ ] **Design System**: Follows Material 3 guidelines and app theme
- [ ] **Loading States**: Shows loading indicator during save operations
- [ ] **Error States**: Clear error messaging without blocking UI
- [ ] **Success Feedback**: Smooth navigation back on successful save

### 📱 Responsive Design
- [ ] **Small Screens**: Works correctly on phones with small screens (< 5 inches)
- [ ] **Orientation**: Functions properly in portrait mode
- [ ] **Safe Areas**: Respects system UI insets and safe areas

## 🧪 Edge Cases & Regression Tests

### ⚡ Performance Tests
- [ ] **Fast Navigation**: No lag when opening/closing editor
- [ ] **Memory**: No obvious memory leaks during repeated use
- [ ] **State Management**: Efficient state updates without unnecessary recompositions

### 🔄 Data Integrity Tests
- [ ] **Concurrent Edits**: Handles potential concurrent edits gracefully
- [ ] **Long Text**: Handles very long titles/descriptions without crashes
- [ ] **Special Characters**: Supports Unicode, emojis, and special characters
- [ ] **Date Boundaries**: Correctly handles edge dates (year boundaries, leap years)

### 🏗️ Architecture Tests
- [ ] **ViewModel Isolation**: TaskEditorViewModel operates independently from TaskViewModel
- [ ] **State Preservation**: Form state survives configuration changes
- [ ] **Error Recovery**: Graceful error handling without crashes

## 🔄 Integration Tests

### 📋 Main Screen Integration
- [ ] **List Refresh**: Main screen updates immediately after task creation/editing
- [ ] **Search Persistence**: Search query maintained after returning from editor
- [ ] **Filter Persistence**: Applied filters remain active after editor use
- [ ] **Selection Mode**: Exiting selection mode doesn't interfere with editor

### 🗃️ Data Consistency
- [ ] **Priority Display**: Created/edited priority levels display correctly in list
- [ ] **Pin Status**: Pin toggle reflected immediately in main list
- [ ] **Tag Integration**: Tags appear as chips in main list after creation/edit
- [ ] **Reminder Scheduling**: Reminders properly scheduled in background

## 📊 Business Logic Tests

### 🎯 Feature Completeness
- [ ] **All Fields Supported**: Title, Description, Tag, Priority, Due Date, Reminder, Pin
- [ ] **Validation Rules**: All original validation rules preserved
- [ ] **Default Values**: Appropriate defaults (Medium priority, no pin)
- [ ] **Change Detection**: Accurate change detection in edit mode

### 🔄 Workflow Tests
- [ ] **Create → Edit**: Can edit newly created tasks immediately
- [ ] **Bulk Operations**: Editor works after bulk operations on main screen
- [ ] **Archive Integration**: Editor not accessible for archived tasks
- [ ] **Search Integration**: Editor accessible from search results

## ✅ Acceptance Criteria

### Must Pass (Blocking Issues)
- [ ] FAB navigation opens task editor screen
- [ ] Task editing via tap navigation works
- [ ] Save disabled until changes made (edit mode)
- [ ] All validation rules preserved
- [ ] Keyboard doesn't cover fields on small devices
- [ ] Navigation back works without duplicates

### Should Pass (Polish Issues)
- [ ] Smooth animations and transitions
- [ ] Consistent Material 3 theming
- [ ] Responsive layout on all screen sizes
- [ ] Clear error messaging

### Nice to Have (Enhancement Opportunities)
- [ ] Keyboard shortcuts work as expected
- [ ] Accessibility features function properly
- [ ] Dark theme support if implemented

---

## 🎯 Testing Instructions

1. **Environment Setup**
   - Test on multiple device sizes (small phones, large phones)
   - Test on Android API levels 26+ (minimum supported)
   - Test with different input methods and keyboards

2. **Testing Approach**
   - Start with happy path scenarios
   - Test error conditions and edge cases
   - Verify integration with existing features
   - Confirm no regressions in main list functionality

3. **Issue Reporting**
   - Note device details for any issues found
   - Include steps to reproduce
   - Classify as blocking, polish, or enhancement
   - Verify issues against requirements

**Note**: This checklist should be completed before considering the Task Editor screen feature complete and ready for release.