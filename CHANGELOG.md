# Changelog

All notable changes to the Task Tracker app will be documented in this file.

## [Unreleased]

### Added
- **Backup & Restore** - Export all tasks (including archived) to JSON or CSV files via Android Storage Access Framework.
- **Import backup** - Restore tasks from a JSON backup file with validation and confirmation dialog.
- **Settings screen** - New settings screen accessible from the main task list top bar with a gear icon.
- **CSV export** - Export tasks as RFC 4180 compliant CSV spreadsheet for use in external tools.
- **Backup validation** - Imported tasks are sanitized (blank titles skipped, priority clamped, timestamps corrected).
- **kotlinx-serialization** - Added kotlinx-serialization-json dependency for structured JSON backup format.
- **ProGuard rules** - Added rules for kotlinx-serialization to ensure release builds work correctly.
- **Unit tests** - Added tests for JsonBackupSerializer, CsvBackupSerializer, and TaskBackupValidator.
