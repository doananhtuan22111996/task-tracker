package dev.tuandoan.tasktracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class],
    version = 6,
    exportSchema = false,
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
}
