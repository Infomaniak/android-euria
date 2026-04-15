import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

/*
 * Infomaniak Euria - Android
 * Copyright (C) 2025 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

plugins {
    alias(core.plugins.android.application) // This line should be 1st, or you'll have Gradle sync issue
    alias(core.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(core.plugins.kapt)
    alias(core.plugins.navigation.safeargs)
    alias(libs.plugins.dagger.hilt)
    alias(core.plugins.sentry.plugin)
}

val appCompileSdk: Int by rootProject.extra
val appTargetSdk: Int by rootProject.extra
val appMinSdk: Int by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra

android {

    namespace = "com.infomaniak.euria"

    compileSdk = appCompileSdk

    defaultConfig {
        applicationId = "com.infomaniak.euria"
        minSdk = appMinSdk
        targetSdk = appTargetSdk
        versionCode = 1_004_001_01
        versionName = "1.4.1"

        buildConfigField("String", "CLIENT_ID", "\"10476B29-7B98-4D42-B06B-2B7AB0F06FDE\"")

        androidResources {
            localeFilters += listOf("en", "de", "es", "fr", "it")
            generateLocaleConfig = true
        }
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "distribution"

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    productFlavors {
        create("standard") {
            dimension = "distribution"
            isDefault = true
        }
        create("fdroid") {
            dimension = "distribution"
        }
    }

    val isRelease = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

    val envProperties = rootProject.file("env.properties")
        .takeIf { it.exists() }
        ?.let { file -> Properties().also { it.load(file.reader()) } }

    val sentryAuthToken = envProperties?.getProperty("sentryAuthToken")
        .takeUnless { it.isNullOrBlank() }
        ?: if (isRelease) error("The `sentryAuthToken` property in `env.properties` must be specified (see `env.example.properties`).") else ""

    sentry {
        autoInstallation.sentryVersion.set(core.versions.sentry)
        org = "sentry"
        projectName = "euria-android"
        authToken = sentryAuthToken
        url = "https://sentry-mobile.infomaniak.com"
        includeDependenciesReport = false
        includeSourceContext = isRelease
        uploadNativeSymbols = isRelease
        includeNativeSources = isRelease
    }
}

dependencies {
    implementation(core.infomaniak.core.common)
    implementation(core.infomaniak.core.auth)
    implementation(core.infomaniak.core.crossapplogin.back)
    implementation(core.infomaniak.core.crossapplogin.front)
    implementation(core.infomaniak.core.fragmentnavigation)
    implementation(core.infomaniak.core.inappreview)
    implementation(core.infomaniak.core.matomo)
    implementation(core.infomaniak.core.network)
    implementation(core.infomaniak.core.onboarding)
    implementation(core.infomaniak.core.sentry)
    implementation(core.infomaniak.core.sharedvalues)
    implementation(core.infomaniak.core.twofactorauth.back)
    implementation(core.infomaniak.core.twofactorauth.front)
    implementation(core.infomaniak.core.ui.compose.basicbutton)
    implementation(core.infomaniak.core.ui.compose.basics)
    implementation(core.infomaniak.core.ui.compose.margin)
    implementation(core.infomaniak.core.ui.compose.theme)
    implementation(core.infomaniak.core.ui.view)
    implementation(core.infomaniak.core.webview)

    "standardImplementation"(core.infomaniak.core.notifications.registration)
    "standardImplementation"(core.firebase.messaging.ktx)

    // Compose
    implementation(platform(core.compose.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.compose.ui.android)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.androidx.work)
    kapt(libs.hilt.android.compiler)
    kapt(libs.hilt.androidx.compiler)
    kapt(libs.room.processing) // TODO[workaround]: Remove when https://github.com/google/dagger/issues/4693 is fixed.

    implementation(libs.kotlinx.serialization.json)
    implementation(core.compose.material3)
    implementation(core.compose.runtime)
    implementation(core.compose.ui.tooling.preview)
    implementation(core.lottie.compose)
    implementation(core.material)
}
