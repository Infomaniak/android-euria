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
    alias(libs.plugins.android.application) // This line should be 1st, or you'll have Gradle sync issue
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
        versionCode = 1_002_000_01
        versionName = "1.2.0"

        buildConfigField("String", "CLIENT_ID", "\"10476B29-7B98-4D42-B06B-2B7AB0F06FDE\"")
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
    implementation(project(":Core"))
    implementation(project(":Core:Auth"))
    implementation(project(":Core:CrossAppLogin:Back"))
    implementation(project(":Core:CrossAppLogin:Front"))
    implementation(project(":Core:FragmentNavigation"))
    implementation(project(":Core:InAppReview"))
    implementation(project(":Core:Matomo"))
    implementation(project(":Core:Network"))
    implementation(project(":Core:Network:Models"))
    implementation(project(":Core:Onboarding"))
    implementation(project(":Core:Sentry"))
    implementation(project(":Core:SharedValues"))
    implementation(project(":Core:TwoFactorAuth:Back:WithUserDb"))
    implementation(project(":Core:TwoFactorAuth:Front"))
    implementation(project(":Core:Ui:Compose:BasicButton"))
    implementation(project(":Core:Ui:Compose:Basics"))
    implementation(project(":Core:Ui:Compose:Margin"))
    implementation(project(":Core:Ui:Compose:Theme"))
    implementation(project(":Core:Ui:View"))
    implementation(project(":Core:WebView"))

    "standardImplementation"(project(":Core:Notifications:Registration"))
    "standardImplementation"(core.firebase.messaging.ktx)

    // Compose
    implementation(platform(core.compose.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.compose.ui.android)
    implementation(libs.hilt.android)
    implementation(libs.hilt.androidx.work)
    kapt(libs.hilt.android.compiler)
    kapt(libs.hilt.androidx.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(core.compose.material3)
    implementation(core.compose.runtime)
    implementation(core.compose.ui.tooling.preview)
    implementation(core.lottie.compose)
    implementation(core.material)
}
