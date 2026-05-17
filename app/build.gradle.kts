import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.time.Instant
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.gradle)
    alias(libs.plugins.firebase.perf.gradle)
}

android {
    namespace = "dev.tuandoan.tasktracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.tuandoan.tasktracker"
        minSdk = 26
        targetSdk = 36
        versionCode = Instant.now().epochSecond.toInt() // Epoch seconds: safe until 2038, always increasing
        versionName = "1.11.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            fun getSecretPropertyFile(rootProject: Project): Properties {
                val signingKeyAlias = "SIGNING_KEY_ALIAS"
                val signingKeystorePassword = "SIGNING_KEYSTORE_PASSWORD"
                val signingKeyPassword = "SIGNING_KEY_PASSWORD"

                val secretPropertiesFile: File = rootProject.file("secret.properties")
                val secretProperties = Properties()
                if (secretPropertiesFile.exists()) {
                    secretProperties.load(FileInputStream(secretPropertiesFile))
                }
                System.getenv(signingKeyAlias)?.let {
                    secretProperties.setProperty(signingKeyAlias, it)
                }
                System.getenv(signingKeystorePassword)?.let {
                    secretProperties.setProperty(signingKeystorePassword, it)
                }
                System.getenv(signingKeyPassword)?.let {
                    secretProperties.setProperty(signingKeyPassword, it)
                }
                return secretProperties
            }

            val secretProperties = getSecretPropertyFile(rootProject)
            signingConfigs {
                create("release") {
                    keyAlias = "${secretProperties["SIGNING_KEY_ALIAS"]}"
                    keyPassword = "${secretProperties["SIGNING_KEY_PASSWORD"]}"
                    storePassword = "${secretProperties["SIGNING_KEYSTORE_PASSWORD"]}"
                    storeFile = File("$rootDir/keystore.jks")
                }
            }

            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")

            // FB-03: enable R8/ProGuard mapping-file upload to Crashlytics so the console
            // can deobfuscate release stack traces — but only when service-account
            // credentials are present in the environment. When `GOOGLE_APPLICATION_CREDENTIALS`
            // is unset (CI, dev builds without a key), the plugin doesn't register the
            // upload task chain at all, so the R8 trace analysis it triggers also doesn't
            // run. That keeps CI/JVM pipelines green and matches the plugin's pre-FB-03
            // skip-when-unconfigured behaviour. Developers opt in by exporting
            // GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json before `bundleRelease`.
            //
            // Reads the env var via `providers.environmentVariable` (configuration-cache
            // friendly) rather than `System.getenv` (which marks the cache invalid).
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled =
                    providers
                        .environmentVariable("GOOGLE_APPLICATION_CREDENTIALS")
                        .isPresent
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Guardrail: fail the build if any file outside the allowlist writes
// `Task(... tag = ...)` or `.copy(... tag = ...)`. All tag writes must go
// through TaskManager (the normalization chokepoint). See TCN-13 / TCN-16.
val checkTagInvariant =
    tasks.register<Exec>("checkTagInvariant") {
        group = "verification"
        description = "Enforce that Task.tag writes go through TaskManager."
        workingDir = rootDir
        commandLine = listOf("bash", "scripts/check-tag-invariant.sh")
        // Incremental: re-run only when a scanned file or the script itself changed.
        inputs.files(fileTree("src/main") { include("**/*.kt") })
        inputs.file(rootProject.file("scripts/check-tag-invariant.sh"))
        outputs.upToDateWhen { true }
    }

tasks.named("check").configure {
    dependsOn(checkTagInvariant)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ViewModel dependency
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt dependencies
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.dagger.hilt.compiler)

    // Navigation dependencies
    implementation(libs.androidx.navigation.compose)

    // Serialization dependencies
    implementation(libs.kotlinx.serialization.json)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // AppCompat (for per-app language support)
    implementation(libs.androidx.appcompat)

    // Google Play In-App Review
    implementation(libs.google.play.review.ktx)

    // Glance (AppWidget)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Calendar (Kizitonwose) — month grid for CalendarScreen. See ADR-001.
    implementation(libs.kizitonwose.calendar.compose)

    // WorkManager dependencies
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Firebase (FB-02) — Crashlytics + Analytics + Performance Monitoring.
    // Collection is disabled by default via AndroidManifest.xml meta-data entries
    // (firebase_*_collection_enabled = false); FB-06's consent gate flips them to
    // true at runtime when the user opts in via Settings → Privacy. The manifest
    // default is the structural safeguard: opt-out = zero network even between
    // FB-02 merge and FB-06 implementation. ADR-003 for the integration architecture.
    // BOM pins all three submodule versions; see `androidx-compose-bom` for the
    // same pattern.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
