// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.dagger.hilt.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/generated/**")

        ktlint("1.4.1").editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "continuation_indent_size" to "4",
                "max_line_length" to "120",
                "ktlint_standard_function-naming" to "disabled",
                "ktlint_standard_parameter-list-wrapping" to "disabled",
                "ktlint_standard_no-wildcard-imports" to "enabled",
                "ktlint_standard_filename" to "disabled",
                "ktlint_standard_value-parameter-comment" to "disabled",
                "ktlint_standard_backing-property-naming" to "disabled",
            ),
        )

        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**")

        ktlint("1.4.1")
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("gradle") {
        target("*.gradle", "**/*.gradle")
        targetExclude("**/build/**")

        trimTrailingWhitespace()
        endWithNewline()
    }
}
