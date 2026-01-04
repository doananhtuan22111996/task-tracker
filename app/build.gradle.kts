import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt.android)
}

android {
    namespace = "dev.tuandoan.tasktracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.tuandoan.tasktracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
        compose = true
    }
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


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}