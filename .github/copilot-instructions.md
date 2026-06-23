# Copilot Coding Agent Onboarding — android-euria

> **Read `AGENTS.md` first** for architecture, conventions, and module structure. This file focuses on build, CI, and validation.

## Overview
Euria is an Android AI-assistant app (sovereign, privacy-first) built with Kotlin + Jetpack Compose. It renders the Euria web app in a WebView with a bidirectional JavaScript bridge and adds native features (Hilt DI, WorkManager, FCM). Two build flavors: `standard` (Google Play, includes Firebase) and `fdroid`.

## One-Time Environment Setup
```bash
git submodule update --init --recursive   # pull Core submodule — required for Gradle settings plugin
cp env.example.properties env.properties  # fill sentryAuthToken (use a dummy value locally)
touch uitest-env.properties               # required by CI; leave empty locally
```
Missing `env.properties` or `uitest-env.properties` → Gradle config phase fails.

## Build
```bash
./gradlew assembleStandardDebug    # standard flavor
./gradlew assembleFdroidDebug      # fdroid flavor (no Firebase)
./gradlew build                    # all variants — same as CI
```

## Tests & Lint (CI: `.github/workflows/android.yml`)
CI runs on every non-draft PR. It runs in order:
```bash
./gradlew clean
./gradlew build
./gradlew testFdroidDebugUnitTest testStandardDebugUnitTest --stacktrace
```
Replicate locally with the same sequence. Draft PRs are skipped by CI.

## Project Layout
```
app/src/main/java/com/infomaniak/euria/
├── data/        # LocalSettings (SharedPreferences), API models
├── di/          # Hilt modules (dispatchers, qualifiers)
├── network/     # ApiRepository
├── services/    # WorkManager workers
├── ui/          # Compose screens; EuriaWebView + JS bridge ("euria" interface name)
Core/            # Git submodule — Infomaniak shared library
gradle/libs.versions.toml   # all dependency versions
settings.gradle.kts         # includes Core/build-logic via pluginManagement
```

## Rules the Agent Must Follow
- All user-visible strings go in `res/values/strings.xml` — never hardcoded.
- Firebase / Google services are `standardImplementation` only — fdroid must compile without them.
- The WebView JS bridge interface name `"euria"` must remain stable.
- `env.properties` is git-ignored — never commit it.
- When adding/removing a runtime dependency, update `LICENSES.md` at the repo root.
