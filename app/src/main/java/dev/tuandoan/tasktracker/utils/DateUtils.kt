package dev.tuandoan.tasktracker.utils

import java.text.SimpleDateFormat
import java.util.*

fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}