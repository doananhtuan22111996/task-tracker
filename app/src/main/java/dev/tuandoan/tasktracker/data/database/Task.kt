package dev.tuandoan.tasktracker.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null, // Timestamp when task was completed (nullable)
    val dueAt: Long? = null, // Nullable epoch millis
    val dueAtHasTime: Boolean = false, // Whether user explicitly set a time (vs default end-of-day)
    val reminderOffsetMinutes: Int? = null, // Nullable reminder offset in minutes (0 = none)
    val tag: String? = null, // Single optional tag for organizing tasks
    val isPinned: Boolean = false, // Pin/unpin tasks to keep important items on top
    val priority: Int = 1, // Priority level: 0=LOW, 1=MEDIUM, 2=HIGH (default MEDIUM)
    val isArchived: Boolean = false, // Archive tasks instead of deleting (soft delete)
    val archivedAt: Long? = null, // Timestamp when task was archived (nullable)
    // Recurrence fields (v1.4.0) — stored as RecurrenceType ordinal
    val recurrenceType: Int = 0, // RecurrenceType: 0=NONE, 1=DAILY, 2=WEEKLY, 3=MONTHLY, 4=YEARLY
    val recurrenceInterval: Int = 1, // Every N periods (e.g., every 2 weeks)
    val recurrenceDaysOfWeek: Int = 0, // Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
    val recurrenceEndDate: Long? = null, // Optional end date epoch millis (null = repeat forever)
    val parentRecurringTaskId: Long? = null, // Links to first task in recurring chain (null = origin)
)
