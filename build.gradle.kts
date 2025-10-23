// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    extra.apply {
        set(
            "appCompileSdk",
            36
        ) // Ensure any extra configChanges are added into Activities' manifests.
        set("appTargetSdk", 35)
        set("appMinSdk", 27)
        set("legacyMinSdk", 27)
        set("javaVersion", JavaVersion.VERSION_17)
    }
    dependencies {
        classpath(libs.navigation.safeargs)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.serialization) apply false
    alias(core.plugins.kapt) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
