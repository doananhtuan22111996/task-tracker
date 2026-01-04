package dev.tuandoan.tasktracker

import android.app.Application
import androidx.room.Room
import dev.tuandoan.tasktracker.data.database.TaskDatabase
import dev.tuandoan.tasktracker.data.repository.TaskRepository
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository

class TaskTrackerApplication : Application() {

    // Database
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java,
            "task_database"
        ).build()
    }

    // Repository
    val taskRepository: ITaskRepository by lazy {
        TaskRepository(database.taskDao())
    }

    // Task Manager
    val taskManager: ITaskManager by lazy {
        TaskManager(taskRepository)
    }
}