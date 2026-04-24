package dev.tuandoan.tasktracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class, Subtask::class],
    version = 12,
    exportSchema = true,
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    abstract fun subtaskDao(): SubtaskDao
}
