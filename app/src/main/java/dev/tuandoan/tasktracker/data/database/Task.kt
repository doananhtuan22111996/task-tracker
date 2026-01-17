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
    val reminderOffsetMinutes: Int? = null, // Nullable reminder offset in minutes (0 = none)
    val tag: String? = null, // Single optional tag for organizing tasks
    val isPinned: Boolean = false, // Pin/unpin tasks to keep important items on top
    val priority: Int = 1, // Priority level: 0=LOW, 1=MEDIUM, 2=HIGH (default MEDIUM)
    val isArchived: Boolean = false, // Archive tasks instead of deleting (soft delete)
    val archivedAt: Long? = null, // Timestamp when task was archived (nullable)
)
