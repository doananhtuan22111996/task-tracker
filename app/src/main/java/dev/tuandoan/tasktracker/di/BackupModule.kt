package dev.tuandoan.tasktracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.tasktracker.data.backup.AndroidBackupFileProvider
import dev.tuandoan.tasktracker.data.backup.BackupFileProvider
import dev.tuandoan.tasktracker.data.backup.BackupSerializer
import dev.tuandoan.tasktracker.data.backup.CsvBackupSerializer
import dev.tuandoan.tasktracker.data.backup.JsonBackupSerializer
import dev.tuandoan.tasktracker.domain.backup.BackupValidator
import dev.tuandoan.tasktracker.domain.backup.TaskBackupValidator
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for the JSON backup serializer.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JsonSerializer

/**
 * Qualifier for the CSV backup serializer.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CsvSerializer

/**
 * Hilt module that provides backup-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    @JsonSerializer
    abstract fun bindJsonSerializer(impl: JsonBackupSerializer): BackupSerializer

    @Binds
    @Singleton
    @CsvSerializer
    abstract fun bindCsvSerializer(impl: CsvBackupSerializer): BackupSerializer

    @Binds
    @Singleton
    abstract fun bindBackupFileProvider(impl: AndroidBackupFileProvider): BackupFileProvider

    @Binds
    @Singleton
    abstract fun bindBackupValidator(impl: TaskBackupValidator): BackupValidator
}
