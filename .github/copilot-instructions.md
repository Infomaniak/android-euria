# Copilot Coding Agent Onboarding — android-euria

> **Read `AGENTS.md` first** for architecture, conventions, and module structure. This file covers build, CI, and validation.

## Overview
Euria is an Android AI-assistant app (sovereign, privacy-first) built with Kotlin + Jetpack Compose. It renders the Euria web app in a WebView with a bidirectional JavaScript bridge and adds native features (Hilt DI, WorkManager, FCM). Two build flavors: `standard` (Google Play, includes Firebase) and `fdroid`.

## One-Time Environment Setup
```bash
git submodule update --init --recursive   # pull Core submodule — required for Gradle settings plugin
cp env.example.properties env.properties  # only required for Sentry release tasks; debug builds work without it
```
`uitest-env.properties` is created automatically by CI and is not required locally.

## Build & Test (CI: `.github/workflows/android.yml`)
CI runs on every non-draft PR:
```bash
./gradlew clean
./gradlew build
./gradlew testFdroidDebugUnitTest testStandardDebugUnitTest --stacktrace
```

## Project Layout
```
app/src/main/java/com/infomaniak/euria/
├── data/        # LocalSettings (SharedPreferences), API models
├── di/          # Hilt modules
├── network/     # ApiRepository
├── services/    # WorkManager workers
├── ui/          # Compose screens; main screen is EuriaMainScreen.kt (uses Core WebView composable + JavascriptBridge with NAME = "euria")
Core/            # Git submodule — Infomaniak shared library
gradle/libs.versions.toml
```

## PR Review Instructions

- Ensure strings are localized via `strings.xml` resources.
- Ensure UI is written in Jetpack Compose using Material3 components.
- `standard` flavor only: Firebase, Google services (`standardImplementation`) — fdroid builds must compile without them.
- The JavaScript bridge interface name `"euria"` (`JavascriptBridge.NAME`) must remain stable — changing it breaks the web app integration.
- `env.properties` is git-ignored — never commit it.
- When adding/removing a runtime dependency, update `LICENSES.md` at the repo root.
