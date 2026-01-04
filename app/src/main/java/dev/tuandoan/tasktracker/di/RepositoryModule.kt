package dev.tuandoan.tasktracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.tasktracker.data.repository.TaskRepository
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import javax.inject.Singleton

/**
 * Hilt module that provides repository and domain layer dependencies.
 * Uses @Binds for efficient interface-to-implementation binding.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds TaskRepository implementation to ITaskRepository interface.
     * @Binds is more efficient than @Provides for simple interface binding.
     */
    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepository: TaskRepository
    ): ITaskRepository

    /**
     * Binds TaskManager implementation to ITaskManager interface.
     * Provides the domain layer service with repository dependency.
     */
    @Binds
    @Singleton
    abstract fun bindTaskManager(
        taskManager: TaskManager
    ): ITaskManager
}