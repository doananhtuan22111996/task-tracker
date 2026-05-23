# Changelog

All notable changes to the Task Tracker app will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com).

## [Unreleased]

### Added
- complete-from-widget: per-row checkbox marks the task done in one tap, reusing `TaskManager.toggleTaskCompletion` so reminder cancellation, recurrence generation and analytics all stay on a single code path (V13-04)

### Changed
- widget supports responsive 2x2 / 4x2 / 4x4 placements (default 4x2), scaffold for v1.13.0 widget v2 (V13-01)
- widget renders size-aware layouts: 2x2 count badge + top task, 4x2 top 5 (unchanged), 4x4 top 10 with overdue header (V13-02)
- widget task row split into two click targets: leading checkbox completes the task, rest of the row opens the editor (V13-04)
- widget row now uses the platform Glance `CheckBox` (native accent color + ripple) instead of a text glyph; OQ-01 strikethrough/fade animation dropped — Glance composables compile to a static RemoteViews snapshot and can't host Compose animations (V13-05)
- widget data provider now dispatches via a sealed `WidgetSource` (Today / Upcoming7d / Pinned / Tag); only Today is wired into the widget surface until V13-09 reads per-widget config (V13-03)

## [1.12.0] - 2026-05-17

### Notes
- **What's new (FB-20):** You can now opt in to help improve the app via Settings → Privacy.

### Removed
- Advertising-related permissions auto-merged by Firebase Analytics (`com.google.android.gms.permission.AD_ID`, `android.permission.ACCESS_ADSERVICES_ATTRIBUTION`, `android.permission.ACCESS_ADSERVICES_AD_ID`, `com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE`) are stripped from the merged manifest via `tools:node="remove"`. Task Tracker doesn't show ads, doesn't use the Advertising ID, and doesn't participate in install attribution — leaving these permissions in the merged manifest would have contradicted the FB-18 Privacy Policy's "never for advertising, profiling, or selling" commitment and required answering "Yes" to advertising-ID collection in the Play Console Data Safety form. Firebase Analytics still functions: it falls back to a Firebase Installation ID, which the policy already discloses (v1.12.0 release-prep)

### Changed
- `data_extraction_rules.xml` (API ≥ 31) and `backup_rules.xml` (API 26-30) now both exclude `privacy.preferences_pb` from cloud-backup so the diagnostics opt-in flag (FB-04) stays on the device that explicitly granted it. New devices start at the documented "OFF by default" state per the Privacy Policy (FB-18) regardless of Android version; cloud restore no longer carries the opt-in across — the user re-affirms explicitly. `task_tracker.db` is still excluded from cloud-backup on both APIs (task content stays on-device by design); device-transfer (API ≥ 31) preserves the database so device-to-device migration works but excludes the privacy preferences. `backup_rules.xml` was previously empty (template default) — closed legacy-API hole (v1.12.0 release-prep)
- Privacy Policy adds explicit Children stance ("not directed at children under 13; we do not knowingly collect data from children under 13") and Contact section (publisher name + privacy email for data-deletion requests) — both required for the Play Console Data Safety form's "data controller / how to contact" question. 4 new English strings appended to the existing FB-18 block; non-EN locales batched to v1.13.0 (v1.12.0 release-prep)

### Added
- Firebase BOM 33.10.0 + Crashlytics/Analytics/Performance Monitoring submodules on the app classpath, along with the `google-services`, `firebase-crashlytics`, and `firebase-perf` Gradle plugins (FB-01 catalog + FB-02 application). SDK collection is disabled by default via `AndroidManifest.xml` meta-data (`firebase_crashlytics_collection_enabled=false`, `firebase_analytics_collection_enabled=false`, `firebase_performance_collection_enabled=false`) — this is the structural opt-out guarantee from ADR-003 and keeps the interim window between FB-02 and FB-06 free of network traffic. FB-06's runtime `setXxxCollectionEnabled(true)` will override the manifest defaults when the user opts in. Release-APK size delta: +0.96 MB (9.85 MB → 10.81 MB), well inside the 15 MB budget (FB-02)
- `PrivacyRepository` (FB-04) — Hilt-injected `@Singleton` persisting the `diagnosticsOptIn: Boolean` flag in its own `privacy.preferences_pb` DataStore file (separate from the existing `settings` file per ADR-003 Option E for audit clarity). Exposes `diagnosticsOptIn: Flow<Boolean>` with `distinctUntilChanged` + `suspend setDiagnosticsOptIn(Boolean)` + a static `readOptInOnce(context)` for the pre-Hilt synchronous read `TaskTrackerApplication.onCreate` needs in FB-06. Default value `false` for fresh installs. 7 new JVM tests covering default/write/toggle/Flow-emission/dedupe/persistence using `PreferenceDataStoreFactory` with a temp file (no Robolectric) (FB-04)
- `PrivacyManager` (FB-05) — single façade over `FirebaseCrashlytics`, `FirebaseAnalytics`, and `FirebasePerformance`. Exposes `initCollectionState(optIn: Boolean)` for the FB-06 startup gate (fans out to all three SDK enable calls; does **not** write to the DataStore) and `suspend setEnabled(optIn: Boolean)` for the Settings toggle (writes persistence AND fans out, plus `Crashlytics.deleteUnsentReports()` on disable to discard queued crash payloads). New `diagnostics/di/DiagnosticsModule.kt` provides the three Firebase SDK singletons since they lack `@Inject` constructors. 11 new MockK-driven JVM tests verifying fan-out, call-order (persist-before-SDK, disable-before-delete), and that the startup path never overwrites the persisted flag (FB-05)
- Consent gate wired into `TaskTrackerApplication.onCreate` (FB-06) — new `@Inject lateinit var privacyManager: PrivacyManager`; `applyDiagnosticsConsent()` runs immediately after `super.onCreate()` and before any other application code, reading the persisted opt-in via `PrivacyRepository.readOptInOnce(this)` and fanning out to the three Firebase SDKs via `PrivacyManager.initCollectionState(optIn)`. Reflects the user's previously persisted choice (or the default `false` on fresh install) back to the SDKs before anything else in the Application lifecycle runs. KDoc on `onCreate` documents the ordering invariant so future refactors don't slip Firebase-touching code ahead of the gate. Cold-start regression measurement lands separately in FB-23 (FB-06)
- **Settings → Privacy** section (FB-07) — new section below Backup & Restore, containing a single Diagnostics toggle ("Send diagnostics") bound to `PrivacyRepository.diagnosticsOptIn`. Flipping the switch routes through `SettingsViewModel.setDiagnosticsOptIn` → `PrivacyManager.setEnabled(optIn)`, which persists the flag AND fans out to Crashlytics / Analytics / Performance atomically per ADR-003; disable path also calls `Crashlytics.deleteUnsentReports()` to discard queued payloads. Default (fresh install / never-opted-in) = off, preserving the structural opt-out guarantee. 3 new English strings (`settings_section_privacy`, `settings_diagnostics`, `settings_diagnostics_desc`); 7 non-EN locale translations land in FB-09. 4 new VM tests covering the initial-default, reactive-reflects-repo, and both delegate paths. No new composable files — `DiagnosticsToggle` added inline in `SettingsScreen.kt` matching the `DynamicColorToggle` pattern (FB-07)
- **"What's collected?" bottom sheet** (FB-08) — new `TextButton` trigger below the diagnostics toggle opens a `ModalBottomSheet` disclosing exactly what each Firebase SDK collects. Three section blocks (Crashlytics / Analytics / Performance) each with plain-language bullets, including explicit `Never:` lines on Analytics ("task titles, descriptions, tags, reminder times") and Performance ("the contents of your screens") — the reassurance users care most about, repeated per section so it doesn't rely on one-read memory. Static content; no ViewModel plumbing. New `ui/components/WhatsCollectedBottomSheet.kt` (~115 lines) follows the `BulkPriorityBottomSheet.kt` convention with a private `SectionBlock` helper. `verticalScroll` on the inner column so long FB-09 translations or large accessibility font sizes don't clip. `semantics { heading() }` on the sheet title + each section header so TalkBack treats them as navigable landmarks. 14 new English strings under the existing `<!-- Privacy -->` block; FB-09 batches translations for FB-07's 3 keys + FB-08's 14 keys = 17 × 7 locales in one pass (FB-08)
- Privacy section translations (FB-09) — 7 non-EN locales (de/es/fr/hi/in/pt/vi) × 17 new keys from FB-07 + FB-08 = **119 new entries**. Firebase product names ("Crashlytics", "Analytics", "Performance Monitoring") kept as loanwords; `Never:` rhetorical structure preserved per locale (`Niemals:` / `Nunca:` / `Jamais :` / `कभी नहीं:` / `Tidak pernah:` / `Nunca:` / `Không bao giờ:`). French colon spacing follows the existing locale convention (regular space). No color/cross-reference claims in any translation per the copy-verification rule. Verified via `./gradlew assembleDebug` — aapt parity holds across all 7 locales (FB-09)
- **Crashlytics custom keys** (FB-10) — on every opt-in (`PrivacyManager.setEnabled(true)` from Settings, `initCollectionState(true)` from cold-start gate), 6 custom keys are written to Crashlytics so later-session crashes carry debug context: `locale` (current per-app language tag), `theme` (LIGHT/DARK/SYSTEM), `dynamic_color` (Material You toggle), `task_count_bucket` (coarse bucket — `0` / `1-9` / `10-49` / `50-199` / `200+` — avoiding raw counts that could identify extreme-value users), `calendar_tab_active` (snapshot-as-false at opt-in; nav-observer-driven dynamic version deferred to follow-up), and `app_version_name` (BuildConfig passthrough — cheap insurance against stale SDK version metadata). New `diagnostics/CrashlyticsKeysWriter.kt` owns the key-assembly concern separately from `PrivacyManager`'s SDK-toggle concern; every throwable in `writeAll()` is swallowed so a DataStore or DB failure never takes the app down from the diagnostics path. Cold-start opt-in writes go async on a new qualified `@DiagnosticsScope CoroutineScope` so `Application.onCreate` stays inside ADR-003's budget; Settings opt-in writes inline since `setEnabled` is already suspend. 12 new `CrashlyticsKeysWriterTest` cases (6 key writes + 5 bucket ranges + 1 throwable-swallow) + 4 new `PrivacyManagerTest` cases covering both opt-in paths and both opt-out paths (FB-10)
- `BreadcrumbLogger` façade (FB-11) — thin `@Singleton` wrapper around `FirebaseCrashlytics.log` exposing `log(category: BreadcrumbCategory, detail: String)`. The typed `BreadcrumbCategory` enum (`NAV`, `TASK_ACTION`, `FILTER`, `BACKUP`, `SETTINGS`, `REMINDER`) prevents call sites from interpolating user data into freeform strings, keeping PII out of Crashlytics payloads per ADR-003. Rendered as `"[CATEGORY] detail"` so console filtering by prefix is trivial. No explicit opt-out gate — `Crashlytics.log` is a documented no-op while collection is disabled by `PrivacyManager`, so gating here would duplicate the SDK contract and add a DataStore read to every UI event. No production callers yet; call sites land in FB-12. 4 new `BreadcrumbLoggerTest` cases (render format, whitespace/punctuation preservation, exhaustive over all 6 enum variants, empty-detail edge case) (FB-11)
- **Breadcrumb instrumentation** (FB-12) — 10 `BreadcrumbLogger` call sites wired across the app so later-session crashes carry a timeline of user actions without any PII. Sites: `NAV` route changes (via Compose `LaunchedEffect(currentRoute)` in `MainActivity`, logged as `route=<constant>` — route constants only, never nav args that could carry task ids); `TASK_ACTION` for `TaskManager.createTask`/`updateTask`/`archiveTask`/`materializeProjectedOccurrence` (flags + opaque ids, never titles/tags/descriptions); `BACKUP` for `ExportBackupUseCase` + `ImportBackupUseCase` start/done/failed (format enum + bucketed counts — `0`/`1-9`/`10-49`/`50-199`/`200+` — and explicitly omits the exception message which may carry file paths); `NAV` for `CalendarViewModel.onDaySelect` (plain verb `calendar_day_tap` without the date); `SETTINGS` for `SettingsViewModel` theme/dynamicColor/locale/diagnostics setters (enum names + booleans + BCP-47 language tags, which match `CrashlyticsKeysWriter.LOCALE` rules); `REMINDER` for `TaskReminderWorker.doWork()` (opaque id; `KEY_TASK_TITLE` is explicitly NOT logged per an in-code comment). Every new call site has a PII-review annotation. 12 new MockK tests — 4 in `TaskManagerTest` (create/update/archive PII-safety), 1 in `TaskManagerRecurrenceTest` (materialize), 4 in `SettingsViewModelTest` (theme/dynamicColor/locale/diagnostics), 1 in `CalendarViewModelTest` (onDaySelect date-omission), 3 in a new `BackupBreadcrumbsTest` (export happy path + export failure + import failure, with explicit assertions that the raw exception message never reaches a breadcrumb). Shared `testutil/BreadcrumbTestUtil.kt` for sites that don't care to assert (relaxed MockK). No production callers beyond these 10; `MainActivity` nav-listener and `TaskReminderWorker` logging are not JVM-unit-testable (require Robolectric / WorkManager runtime) and are covered by FB-21 network-silence integration + manual device checks (FB-12)
- `AnalyticsLogger` façade + `AnalyticsEvent` sealed class (FB-13) — typed wrapper around `FirebaseAnalytics.logEvent` covering the 12 v1.12.0 events from PRD FR-15 (task_created/completed/archived/restored, calendar_tab_opened/day_tapped/recurrence_materialized, subtask_added, bulk_operation_applied, backup_exported/imported, help_faq_expanded). Every event is a sealed subtype so callers can't pass freeform strings; count params (task_count, selection_size, record_count) accept `Int` but serialize as the 5-bucket string shared with `CrashlyticsKeysWriter.task_count_bucket` (FB-10) and FB-12 backup breadcrumbs via `diagnostics/TaskCountBucket.kt` — one source of truth for the bucketing rule. `op_type`, `format`, and `outcome` are enum-backed (`BulkOpType`, `BackupEventFormat`, `BackupOutcome`) so a UI-supplied string can't leak through. Subtypes expose params as a typed `Map<String, Any>`; the façade converts to `Bundle` inside `log()` so JVM unit tests can assert on the map directly (no Robolectric needed — matches the project's FB-06 `Log` tripwire precedent). The 3 Firebase auto-logged events (`app_open`, `screen_view`, `first_open`) are intentionally NOT in the sealed class — they fire from SDK auto-init and we don't want a second code path. No production callers yet; call sites land in FB-14. 18 new `AnalyticsLoggerTest` cases (one per event + exhaustive enum coverage + negative-invariant assertions that no event carries `title`/`tag`/`description`/`date`/raw-int counts) (FB-13)
- `PerformanceLogger` façade + `PerformanceTraceName` sealed class (FB-16) — typed wrapper around `FirebasePerformance.newTrace` mirroring the `BreadcrumbLogger` (FB-11) and `AnalyticsLogger` (FB-13) discipline. Sealed `PerformanceTraceName` closes the trace catalog (`CalendarMonthRender`, `BackupImport` — pre-staged for FB-17) so callers can't ship arbitrary trace names that explode the Firebase console schema. Returns a `PerformanceTrace` interface (not the SDK's `Trace` directly) so JVM tests can substitute fakes without Robolectric. No explicit opt-out gate — `FirebasePerformance` honors `isPerformanceCollectionEnabled` toggled by `PrivacyManager`, same SDK-contract convention used by Breadcrumb/Analytics. 5 new `PerformanceLoggerTest` cases (start orders newTrace + start, stop forwards, putAttribute forwards, exhaustive trace-name catalog pin)
- **In-app Privacy Policy** (FB-18) — new `PrivacyPolicyScreen` reachable from Settings → Privacy → Privacy Policy. Static, scrollable, read-only screen with 8 sections covering: what we collect (broken out by Crashlytics / Analytics / Performance), why, retention windows (Analytics/Performance 90 days, Crashlytics indefinite), opt-out mechanism, third-party processing (Google as data processor), the no-account/no-ads/works-offline posture, change-policy, and effective date. Every claim verifiable against code (e.g. on-disable `deleteUnsentReports` matches FB-05's `PrivacyManager.setEnabled(false)` path; `Never:` invariants for Analytics/Performance match FB-13/14 + FB-08 sheet content). Section headers carry `semantics { heading() }` for TalkBack heading-navigation. New `PRIVACY_POLICY` route + Settings ListItem entry below the FB-08 "What's collected?" sheet trigger (long-form policy below short-answer sheet — same subject, different depth). 17 new English strings under a `<!-- Privacy Policy (FB-18 — 2026-05-17; non-EN locales land in v1.13.0) -->` block. Effective for v1.12.0; non-EN locales batched as a v1.13.0 i18n ticket per the FB-09 precedent. Closes Epic G ticket 1 of 3 (FR-19); FB-19 (Play Console Data Safety form) and FB-20 (release-notes line) are operational and ship outside this PR (FB-18)
- Crashlytics mapping-file upload wired for release builds (FB-03) — `firebase.crashlytics { mappingFileUploadEnabled = ... }` on the `release` build type is now conditional on the `GOOGLE_APPLICATION_CREDENTIALS` env var being set. When the env var is unset (CI, dev builds without a service-account key), the plugin doesn't register the upload task chain at all, so the R8 trace analysis it triggers also doesn't run — keeps CI/JVM pipelines green. When set, the mapping file uploads to Firebase Crashlytics and the console can deobfuscate release stack traces. Closes Epic A. The Firebase Tools SDK reads the env var directly (Gradle can't bridge env vars to non-Exec tasks). README updated with the local opt-in setup (export `GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json` before `bundleRelease`). No runtime code touched; release-build concern only (FB-03)
- **`backup_import` custom Performance trace** (FB-17) — wraps `ImportBackupUseCase.execute()` end-to-end (file read → JSON deserialize → validator → DB replace). The trace starts before file read and stops in `finally` so both the success and error paths produce a final, complete sample. `record_count` attribute carries the bucketed valid-count using the shared `bucketTaskCount` helper (same 5-bucket rule as FB-10 Crashlytics keys, FB-12 breadcrumbs, FB-13 Analytics) so the Firebase Performance console can slice latency by import size without leaking exact counts; on error the attribute is `"0"` matching the zero-rows-imported reality. Reuses the FB-16 `PerformanceLogger` façade + `PerformanceTraceName.BackupImport` (already pre-staged in PR #129) — no façade changes. 3 new `BackupBreadcrumbsTest` cases: success path verifies start → putAttribute("record_count", "1-9") → stop ordering, failure path verifies start → putAttribute("record_count", "0") → stop ordering with a negative invariant that no attribute key/value contains the exception message ("alice"); also exhaustive trace-name pin via the success-path `verifyOrder`. `ImportBackupSubtaskTest` constructor updated to pass `fakePerformanceLogger()`. Device-in-loop verification (kick off a JSON import, confirm `backup_import` trace appears in console with `record_count` attribute) bundled with the FB-15 + FB-23 device verification batch (FB-17)
- **`calendar_month_render` custom Performance trace** (FB-16) — wraps `CalendarScreen` month-change render via `rememberMonthRenderTrace`. Trace measures `visibleMonth` change → `decorationsFlow` re-emits for the new month + one paint frame, covering the chevron / swipe / today-click / saved-state-restore paths. Initial composition deliberately does NOT start a trace — the seeded `visibleMonth` has no "next" emission to settle on, and cold-start is FR-17's `_app_start` territory. `CalendarViewModel` exposes `startMonthRenderTrace()` so the screen calls a typed VM method instead of reaching through the dependency, mirroring how `breadcrumbLogger` and `analyticsLogger` are wired. `DisposableEffect` cleans up an in-flight trace if composition leaves before settle (e.g. user switches tabs mid-load). New `fakePerformanceLogger()` testutil + 1 VM test asserting `startMonthRenderTrace` delegates to `PerformanceLogger.start(CalendarMonthRender)`. Screen-level `LaunchedEffect` is JVM-untestable per project convention; covered by the device-in-loop DoD (trace visible after 3 month swipes in Firebase Performance console) (FB-16)
- **Analytics call-site instrumentation** (FB-14) — 12 `AnalyticsLogger` call sites wired per PRD FR-15. Sites: `task_created` (TaskManager.createTask, priority clamped via AnalyticsPriority), `task_completed` (fires on completing direction of toggleTaskCompletion + markTaskComplete idempotent guard, NOT on un-complete), `task_archived` (single-task archive path), `task_restored` (unarchiveTask), `calendar_tab_opened` (MainActivity `LaunchedEffect(currentRoute)` piggybacking on FB-12's nav listener — fires on route transition, de-duped per tab switch not per VM init), `calendar_day_tapped` (CalendarViewModel.onDaySelect with `has_dated_tasks_that_day` peeked from decorations cache, no date leaked), `calendar_recurrence_materialized` (TaskManager.materializeProjectedOccurrence success path only — idempotent early-return paths don't emit), `subtask_added` (SubtaskUseCase.addSubtask success-only, blank-validation failures don't emit), `bulk_operation_applied` (TaskBulkActionManager.executeBulkOperation on Success + inline at bulkDelete/bulkArchive/bulkPermanentDelete paths; `op_type` passed enum-typed, `selection_size` bucketed via AnalyticsEvent), `backup_exported` (ExportBackupUseCase success; no event on failure per PRD — breadcrumb captures the attempt), `backup_imported` (ImportBackupUseCase fires on all outcomes — `SUCCESS`/`PARTIAL` when skippedCount > 0 / `ERROR` with recordCount=0 and no exception-message leak), `help_faq_expanded` (HelpScreen via hoisted `onFaqExpanded: (HelpFaqSection) -> Unit` callback; fires on expand direction only; enum-typed section means localized titles can never reach Firebase). Helper `BackupFormat.toAnalytics()` maps the domain enum to the diagnostics enum without cross-module coupling. New tests — 6 in TaskManagerTest (TaskCreated shape + TaskArchived/TaskRestored + toggleTaskCompletion fires on complete-only + markTaskComplete idempotent), 2 in TaskManagerRecurrenceTest (materialize success + idempotent-no-op negative invariant), 1 in CalendarViewModelTest (no `date` key in CalendarDayTapped params), 2 in SubtaskUseCaseTest (success-fires + blank-validation-skips), 3 new Analytics cases in BackupBreadcrumbsTest (export success event, export failure no-event, import error with negative-invariant that no event param contains `"alice"`). `MainActivity` nav listener `CalendarTabOpened` and `HelpScreen` Compose callback not JVM-unit-testable; device-verified via Firebase Analytics debug view. (FB-14)

### Fixed
- `PrivacyRepository` now degrades gracefully when the `privacy.preferences_pb` file is unreadable (disk full, corrupt, filesystem permission drift). Both the reactive `diagnosticsOptIn: Flow<Boolean>` and the startup-critical static `readOptInOnce(context)` now catch `IOException` from DataStore and fall back to `false` (the opt-out default per ADR-003), which prevents `TaskTrackerApplication.onCreate` from crashing the app over a diagnostics-layer disk glitch. Non-IO throwables (e.g. future schema bugs) still propagate so real programmer errors surface. 3 new JVM tests using injected throwing `DataStore<Preferences>` doubles (FB-06 self-review follow-up)

### Tooling
- `scripts/measure-cold-start.sh` — dev-only cold-start regression harness for FB-23. Wraps `adb shell am start -W -S` in a 10-iteration loop with pre-flight adb + package checks, a discarded warm-up run, and a summary block reporting `TotalTime` / `WaitTime` median + p95. Output line is paste-ready for the FB-23 Notion page. Zero APK-size / build-time impact; not wired into Gradle. Budgets per ADR-003: opt-out baseline + 100 ms, opt-in baseline + 300 ms; breach triggers escalation to ADR-003 Option B (`@EntryPoint` async init) (FB-23)

## [1.11.0] - 2026-05-09

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
- `CalendarPerfFixtureGenerator` JUnit runner in `src/test` that emits a 1,000-task schema-v3 JSON payload to `app/build/perf/calendar-1000-tasks.json` via the existing `JsonBackupSerializer`. Deterministic via `Random(42)`; shape is 900 concrete + 80 recurring parents (20 DAILY / 40 WEEKLY Mon+Wed+Fri / 20 MONTHLY) + 20 archived, with a realistic priority distribution and ~30% tag density. Zero production-code footprint — consumed by the CAL-32 Perfetto trace test plan through the existing Settings → Restore path on a debug device (CAL-32)

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
