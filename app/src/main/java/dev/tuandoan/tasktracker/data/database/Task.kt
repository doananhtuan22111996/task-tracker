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
    val dueAt: Long? = null, // Nullable epoch millis
    val reminderOffsetMinutes: Int? = null // Nullable reminder offset in minutes (0 = none)
)