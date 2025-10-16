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

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // mavenLocal() // Only used when we want to use a local version of a library (./gradlew publishToMavenLocal)
        maven(url = "https://jitpack.io")
        maven(url = "https://s3.amazonaws.com/tgl.maven")
    }
    versionCatalogs {
        create("core") { from(files("Core/gradle/core.versions.toml")) }
    }
}

rootProject.name = "euria"
include(
    ":app",
    ":Core:AppIntegrity",
    ":Core:Auth",
    ":Core:Avatar",
    ":Core:Coil",
    ":Core:Compose:BasicButton",
    ":Core:Compose:Basics",
    ":Core:Compose:Margin",
    ":Core:Compose:MaterialThemeFromXml",
    ":Core:CrossAppLogin:Back",
    ":Core:CrossAppLogin:Front",
    ":Core:FragmentNavigation",
    ":Core:Legacy",
    ":Core:Legacy:Stores",
    ":Core:Matomo",
    ":Core:Network",
    ":Core:Network:Models",
    ":Core:Onboarding",
    ":Core:Sentry",
    ":Core:WebView",
)
