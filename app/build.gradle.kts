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
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(core.plugins.kapt)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.dagger.hilt)
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
        versionCode = 1
        versionName = "0.0.1"

        buildConfigField("String", "CLIENT_ID", "\"10476B29-7B98-4D42-B06B-2B7AB0F06FDE\"")
        buildConfigField("String", "EURIA_URL", "\"https://euria.infomaniak.com/\"")
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlin {
        jvmToolchain(javaVersion.toString().toInt())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(project(":Core"))
    implementation(project(":Core:Auth"))
    implementation(project(":Core:Compose:BasicButton"))
    implementation(project(":Core:Compose:Basics"))
    implementation(project(":Core:Compose:Margin"))
    implementation(project(":Core:CrossAppLogin:Back"))
    implementation(project(":Core:CrossAppLogin:Front"))
    implementation(project(":Core:FragmentNavigation"))
    implementation(project(":Core:Auth"))
    implementation(project(":Core:Network"))
    implementation(project(":Core:Network:Models"))
    implementation(project(":Core:Onboarding"))
    implementation(project(":Core:SharedValues"))
    implementation(project(":Core:Sentry"))
    implementation(project(":Core:TwoFactorAuth:Back"))
    implementation(project(":Core:TwoFactorAuth:Front"))
    implementation(project(":Core:WebView"))

    // Compose
    implementation(platform(core.compose.bom))
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
