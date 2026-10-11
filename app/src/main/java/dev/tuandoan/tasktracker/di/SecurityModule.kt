package dev.tuandoan.tasktracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.tasktracker.data.security.PlayIntegrityRepositoryImpl
import dev.tuandoan.tasktracker.domain.security.repository.IntegrityRepository
import javax.inject.Singleton

/**
 * Hilt module binding security and integrity repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindIntegrityRepository(impl: PlayIntegrityRepositoryImpl): IntegrityRepository
}
