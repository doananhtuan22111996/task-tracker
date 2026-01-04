package dev.tuandoan.tasktracker.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.tasktracker.data.database.TaskDao
import dev.tuandoan.tasktracker.data.database.TaskDatabase
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 * Uses @Provides for complex object creation that requires custom logic.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides a singleton instance of TaskDatabase.
     * Uses Room.databaseBuilder for database creation with proper configuration.
     */
    @Provides
    @Singleton
    fun provideTaskDatabase(
        @ApplicationContext context: Context
    ): TaskDatabase {
        return Room.databaseBuilder(
            context = context.applicationContext,
            klass = TaskDatabase::class.java,
            name = "task_database"
        )
            // Enable database inspection in debug builds
            .build()
    }

    /**
     * Provides TaskDao from the database.
     * No @Singleton needed here as it's tied to the singleton database instance.
     */
    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao {
        return database.taskDao()
    }
}