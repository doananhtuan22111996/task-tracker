package dev.tuandoan.tasktracker.utils

import java.text.SimpleDateFormat
import java.util.*

fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun formatDueDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd 'at' HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun isOverdue(dueAt: Long): Boolean = dueAt < System.currentTimeMillis()
