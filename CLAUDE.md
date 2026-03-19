# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About Me

- **Role**: Senior Mobile Engineer (Android + iOS) + Python AI Engineer
- **Work mode**: Solo indie developer — I own every stage end-to-end
- **Context**: Personal projects only. Company work is managed separately.
- **Notion**: Each project has its own Notion space for all docs, tasks, and decisions.

## Communication Style

- **Language**: English for everything — code, docs, commits, PRDs, design notes, Notion
- Direct and concise — no fluff, no essay explanations
- Code blocks for all code, commands, file paths
- Multiple options? State trade-offs briefly, then recommend best one
- Never explain obvious things

## Project Overview

Task Tracker is an offline-first Android task management app. Single-module Kotlin project using Jetpack Compose, Material 3, Room, Hilt, and WorkManager.

- **Package:** `dev.tuandoan.tasktracker`
- **Min SDK:** 26 · **Compile/Target SDK:** 36
- **Version:** defined in `app/build.gradle.kts` (`versionName`)
- **Version catalog:** `gradle/libs.versions.toml`
- **Stage:** Closed Beta (Android)

## Tech Stack

```
Language:      Kotlin
UI:            Jetpack Compose + Material 3
Architecture:  Clean Architecture + MVVM (unidirectional data flow)
DI:            Hilt
Async:         Coroutines + Flow
Local DB:      Room
Navigation:    Compose Navigation
Serialization: kotlinx.serialization (JSON backup), CSV (RFC 4180)
Background:    WorkManager (reminders)
Testing:       JUnit4/5, Fakes (not mocks), Turbine, runTest
Build:         Gradle KTS + version catalog (libs.versions.toml)
Formatting:    Spotless + ktlint 1.4.1
```

## Build & Development Commands

```bash
# Build
./gradlew assembleDebug

# Run all unit tests (263+ tests, JVM-only, no emulator needed)
./gradlew testDebugUnitTest

# Run a specific test class
./gradlew testDebugUnitTest --tests "dev.tuandoan.tasktracker.domain.TaskManagerTest"

# Code formatting (Spotless + ktlint 1.4.1) — run before every commit
./gradlew spotlessApply

# Check formatting without fixing (CI use)
./gradlew spotlessCheck

# Full pre-commit check
./gradlew spotlessApply && ./gradlew testDebugUnitTest && ./gradlew assembleDebug
```

## Architecture

Clean Architecture + MVVM with unidirectional data flow:

```
Data (Room/DataStore) → Domain (UseCases/Services) → UI (ViewModels/StateManagers → Compose)
```

**Data layer** — Room database (`TaskDao`, `TaskDatabase`), backup serializers (JSON/CSV), `WorkManagerTaskReminderScheduler`, `SettingsRepository` (DataStore)

**Domain layer** — Pure Kotlin, no Android deps. Use cases (`TaskCrudUseCase`, `TaskFormUseCase`, `TaskFilterUseCase`, `TaskSearchUseCase`), `TaskSortService`, backup use cases (`ExportBackupUseCase`, `ImportBackupUseCase`), `TaskBackupValidator`

**UI layer** — Compose screens (`TaskListScreen`, `TaskEditorScreen`, `ArchivedScreen`, `StatsScreen`). ViewModels are thin coordinators that delegate to state managers (`TaskListStateManager`, `TaskFormStateManager`) and `TaskCrudManager`. Navigation via Compose Navigation.

**DI** — Hilt throughout. Modules in `di/` package.

**Background work** — `TaskReminderWorker` (WorkManager) for local notification reminders.

## Key Conventions

- **Formatting:** Spotless + ktlint. Max line length 120. 4-space indent. No wildcard imports. Compose parameter wrapping rules disabled.
- **Testing:** Fakes over mocks (`FakeTaskRepository`, `FakeReminderScheduler` in `testutil/`). `TestTaskFactory` for test data. Coroutine tests use `runTest`. Flow tests use Turbine. All tests are JVM-only.
- **Strings:** All user-facing text in `res/values/strings.xml`. Use `stringResource()` in Compose, `context.getString()` elsewhere. Zero hardcoded strings.
- **i18n:** 8 locales (en + de/es/fr/hi/in/pt/vi). Locale files at `res/values-<locale>/strings.xml`. Locale registry in `res/xml/locales_config.xml`.
- **Database migrations:** Room with explicit migrations. Current schema includes fields for archiving (`isArchived`, `archivedAt`), completion tracking (`completedAt`), tags, priority, pinning, due dates, and reminders.
- **Serialization:** `kotlinx.serialization` for JSON backup. CSV export is RFC 4180 compliant.

## Core Engineering Principles

1. **Production quality** — assume this fails in prod; handle it now
2. **Security first** — auth, data exposure, input validation; EncryptedSharedPreferences for sensitive data
3. **Offline-first** — local cache, sync strategy, graceful degradation
4. **Clean architecture** — domain layer owns business logic; zero platform deps in domain
5. **Test coverage** — unit test business logic and ViewModels; skip trivial boilerplate
6. **Incremental** — small focused PRs; never big bang rewrites
7. **Consistency** — follow existing project conventions before introducing new patterns
8. **Explicit over clever** — readable code beats smart code

## Git Conventions

```
Branch:
  feat/<short-description>
  fix/<short-description>
  chore/<short-description>
  docs/<short-description>
  test/<short-description>
  refactor/<short-description>
  i18n/<short-description>

Commit (Conventional Commits):
  feat(scope): description
  fix(scope): description
  chore(scope): description
  docs(scope): description
  refactor(scope): description
  test(scope): description
  perf(scope): description
  i18n: description

PR title  = same style as commit
PR body   = what / why / how / test plan
```

Always run `./gradlew spotlessApply` before committing. All tests must pass. One PR per feature/fix, base branch `main`. Never commit directly to `main` during implementation — use feature branches.

## Code Review Checklist

Before any code is considered ready:
- [ ] Logic correct + all edge cases handled
- [ ] No sensitive data in logs, error messages, or API responses
- [ ] Auth and permission checks in place
- [ ] No force unwrap (`!!`) without explicit justification
- [ ] Error handling: network, DB, and unknown/unexpected errors
- [ ] No scope/memory leaks (lifecycle-aware)
- [ ] No unnecessary recompositions or N+1 queries
- [ ] Unit tests cover critical business logic
- [ ] Naming is clear and self-documenting

## Development Workflow

Every feature goes through these stages in order:

```
STAGE 1  IDEATION       → raw idea, problem statement, goals
STAGE 2  PRD            → requirements, user stories, acceptance criteria → Notion
STAGE 3  BREAKDOWN      → epics → features → milestones → release plan → Notion
STAGE 4  TASKS          → actionable tasks with estimates → Notion (tracked)
STAGE 5  DESIGN         → UI/UX concepts, component design, screen flows → Notion
STAGE 6  ARCHITECTURE   → approach decision, patterns, trade-offs, ADR → Notion
STAGE 7  IMPLEMENTATION → story-by-story, spotlessApply + tests after each
STAGE 8  CODE REVIEW    → correctness, security, accessibility, perf
STAGE 9  TESTING        → unit tests, manual plan, device matrix
STAGE 10 GIT & PR       → Conventional Commits, PR description, CI pass, merge
STAGE 11 RELEASE        → versioning, changelog, rollout plan
STAGE 12 DOCUMENTATION  → document all learnings, decisions, changes → Notion
```

Gate approvals required before design (Gate A), before coding (Gate B), and before release (Gate C).

## Notion Documentation Standard

Every doc saved to Notion:
- **Title format**: `[TaskTracker] [DocType] — [Feature/Topic]`
  e.g. `[TaskTracker] PRD — Recurring Tasks`
- **Date**: created + last updated
- **Status**: Draft | In Review | Approved | Archived
- **TL;DR**: 2-3 line summary at top
- **Decisions**: explicitly call out decisions made and WHY
- Tables and headers for structure

## Available Slash Commands

### Shared (platform-agnostic)
```
/idea       → Capture and evaluate a raw idea
/prd        → Write a Product Requirements Document
/breakdown  → Break a feature/epic into tasks + estimates
/roadmap    → Plan milestones and release timeline
/design     → UI/UX design concept: flows, components, screens
/arch       → Architecture Decision Record (ADR)
/pr         → Generate PR title + body
/release    → Generate release notes for a version
/doc        → Format notes/decisions into Notion-ready documentation
/standup    → Generate standup update from current context
```

### Android
```
/impl       → Android implementation plan (Kotlin/Compose/MVVM)
/review     → Android self-code review checklist
/test       → Android test plan (JUnit + Fakes + Turbine + Compose)
/debug      → Android debug session (ADB, Logcat, Coroutines)
/perf       → Android performance audit (Compose, Room, memory)
/deps       → Gradle dependency management (libs.versions.toml)
```

### Python / AI
```
/py-impl    → Python/AI implementation plan (FastAPI + Ollama)
/py-review  → Python/AI code review (async, security, prompt injection)
/py-test    → Python test plan (pytest + asyncio + mocked Ollama)
/py-debug   → Python/AI debug session (async, Ollama, streaming)
/py-perf    → Python/AI performance audit (TTFT, async, memory)
/py-deps    → Python dependency management (uv / pyproject.toml)
```

## Default Behavior

When receiving any task I automatically:
1. Read project context — understand conventions first
2. Identify platform context from file types or explicit mention
3. Explore relevant files before writing any code
4. Think about edge cases, error states, and failure paths
5. Follow existing patterns; introduce new ones only when clearly better
6. Use English for all output

## When In Doubt

- Explore first, code second
- Ask 1 specific question, not multiple vague ones
- For multiple approaches: state trade-offs briefly, recommend best option
- If scope is unclear: make smallest correct change, flag what's left
