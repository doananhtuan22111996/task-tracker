package dev.tuandoan.tasktracker.widget.model

data class WidgetTask(
    val id: Long,
    val title: String,
    val dueAt: Long?,
    val dueAtHasTime: Boolean,
    val priority: Int, // 0=LOW, 1=MEDIUM, 2=HIGH
    val isPinned: Boolean,
)
