package dev.tuandoan.tasktracker.domain.model

data class StreakStats(
    val activeRecurringCount: Int,
    val bestCurrentStreak: TaskStreak?,
    val allTimeBestStreak: TaskStreak?,
)

data class TaskStreak(val taskTitle: String, val taskId: Long, val currentStreak: Int, val longestStreak: Int)
