package dev.tuandoan.tasktracker.diagnostics.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.FirebasePerformance
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the three Firebase SDK singletons (v1.12.0, FB-05).
 *
 * The SDKs are third-party classes without `@Inject` constructors, so Hilt can't
 * auto-bind them. Each `@Provides` function delegates to the SDK's own singleton
 * accessor; the collection-enabled state of each is managed by [PrivacyManager] at
 * runtime and by `firebase_*_collection_enabled=false` meta-data in the manifest at
 * compile time (see `AndroidManifest.xml` — FB-02).
 *
 * [PrivacyManager] depends on all three; no separate `@Provides` for
 * `PrivacyManager` itself since it uses `@Inject constructor`.
 */
@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsModule {

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(): FirebaseAnalytics = Firebase.analytics

    @Provides
    @Singleton
    fun provideFirebasePerformance(): FirebasePerformance = FirebasePerformance.getInstance()
}
