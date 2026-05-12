package dev.tuandoan.tasktracker.diagnostics

/**
 * Single source of truth for the 5-bucket task-count scheme used across the diagnostics
 * surface (v1.12.0). Keeps `CrashlyticsKeysWriter.task_count_bucket` (FB-10) and the
 * `BACKUP export done count=…` / `import done count=…` breadcrumbs (FB-12) reading off the
 * same rule so a future change to the ranges doesn't drift between sites.
 *
 * Ranges chosen to match typical-usage distribution while refusing to ship raw counts at
 * the extremes (a raw 10_000 task count + timestamp could re-identify a single user).
 */
internal fun bucketTaskCount(count: Int): String = when {
    count <= 0 -> "0"
    count < 10 -> "1-9"
    count < 50 -> "10-49"
    count < 200 -> "50-199"
    else -> "200+"
}
