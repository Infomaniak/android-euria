# Euria Android

## Project Snapshot

Modular Android application for Euria (an AI chat assistant) by Infomaniak. Uses MVVM architecture with Hilt DI, Jetpack Compose UI, and a WebView to render the Euria web app. Supports 2 build flavors (standard, fdroid) via Gradle composite builds with the Infomaniak Core library.

**Language**: Kotlin (100%), Jetpack Compose (primary UI), minimal XML

**Key Architecture**: MVVM + WebView-centric, Hilt dependency injection, JavaScript bridge for native↔web communication

## Repository Structure

```
android-euria/
├── Core/                    # Git submodule — shared Infomaniak library (see Core/AGENTS.md if present)
│   └── build-logic/        # Composite build — Gradle convention plugins (included via pluginManagement)
├── app/                     # Main Euria application
│   └── src/
│       ├── main/java/com/infomaniak/euria/
│       │   ├── data/               # Local persistence (LocalSettings via SharedPreferences) and API models
│       │   ├── di/                 # Hilt DI modules (CoroutineDispatchers)
│       │   ├── network/            # ApiRepository (extends Core's ApiRepositoryCore)
│       │   ├── services/           # WorkManager workers (DeviceInfoUpdateWorker)
│       │   ├── ui/                 # Compose screens and components
│       │   │   ├── components/     # Reusable Compose components (buttons, dialogs)
│       │   │   ├── login/          # Onboarding and cross-app login screens
│       │   │   ├── noNetwork/      # No-network error screen
│       │   │   ├── theme/          # EuriaTheme, colors, typography, shapes
│       │   │   └── widget/         # App widget (EuriaAppWidgetProvider)
│       │   ├── upload/             # File/image upload manager (UploadManager)
│       │   ├── utils/              # AccountUtils, WebViewUtils, OkHttpClientProvider, extensions
│       │   ├── webview/            # JavascriptBridge, CustomWebViewClient, CustomWebChromeClient
│       │   ├── MainActivity.kt     # Single-activity entry point, owns the WebView lifecycle
│       │   ├── MainApplication.kt  # @HiltAndroidApp, initialises Core modules
│       │   ├── MainViewModel.kt    # @HiltViewModel, user state & WebView interaction
│       │   ├── EuriaUrls.kt        # Centralised URL constants (env-aware)
│       │   └── MatomoEuria.kt      # Analytics tracking
│       └── standard/               # Flavor-specific sources (Firebase push notifications)
├── build.gradle.kts                # Root build — SDK versions, shared extras
├── settings.gradle.kts             # Includes :app, composite build for Core/build-logic
├── env.example.properties          # Template for local env config (Sentry token)
└── AGENTS.md                       # This file
```

## Build Commands

```bash
# Build standard debug APK
./gradlew :app:assembleStandardDebug

# Build F-Droid debug APK
./gradlew :app:assembleFdroidDebug

# Run unit tests
./gradlew :app:test

# Check compilation only (fast feedback)
./gradlew :app:compileStandardDebugKotlin

# Clean
./gradlew clean

# Install on connected device
./gradlew :app:installStandardDebug
```

## Universal Conventions

- **Kotlin style**: Official Kotlin style guide, enforced via `.editorconfig` (from Core)
- **Commit format**: Conventional commits recommended
- **Branch strategy**: Feature branches → main; open an issue before a PR
- **PR requirements**: CI must pass; one review required for significant changes
- **Never commit**: `env.properties`, API tokens, `local.properties`, or any credentials

## Security & Secrets

- `env.properties` is gitignored; copy from `env.example.properties` and fill in your Sentry auth token
- Authentication tokens are managed by `Core:Auth` (Room-backed `UserDatabase`) via `AccountUtils`
- The WebView authenticates the user by injecting an `USER-TOKEN` cookie via `WebViewUtils.setTokenToCookie()`
- Never log or surface raw `ApiToken` / `accessToken` values in non-debug builds

## Architecture Deep Dive

### Single-Activity / WebView Model

Euria is a **thin Android shell** around a web application hosted at `https://euria.infomaniak.com`.

- `MainActivity` hosts a single full-screen Compose `WebView` (from `Core:Webview`)
- The web app and the native shell communicate via a `JavascriptBridge` (registered as `window.euria`)
- Native → Web: `WebView.evaluateJavascript(...)` calls JS functions (`goTo`, `prepareFilesForUpload`, `fileUploadDone`, `fileUploadError`, etc.)
- Web → Native: `@JavascriptInterface` methods in `JavascriptBridge` (`logIn`, `logout`, `ready`, `openCamera`, `cancelFileUpload`, etc.)

### MVVM Pattern

| Layer | Class | Responsibility |
|-------|-------|----------------|
| View | `MainActivity`, `EuriaMainScreen`, Compose screens | UI rendering, system events |
| ViewModel | `MainViewModel`, `CrossAppLoginViewModel` | State management, business logic |
| Data/Utils | `AccountUtils`, `LocalSettings`, `ApiRepository`, `UploadManager` | Persistence, network, uploads |

### State Management

- ViewModel state exposed as `StateFlow` and consumed with `collectAsStateWithLifecycle()`
- One-shot events use `Channel(Channel.CONFLATED)` (e.g. `webViewQueries`, `cameraLaunchEvents`, `filesToShare`)
- Compose `mutableStateOf` is used for transient UI flags directly on the ViewModel (e.g. `launchMediaChooser`, `hasSeenWebView`)

### Dependency Injection

- All Hilt entry points use `@AndroidEntryPoint` / `@HiltViewModel`
- `CoroutinesDispatchersModule` provides `@IoDispatcher`, `@MainDispatcher`, `@DefaultDispatcher` qualifiers
- `LocalSettings` is `@Singleton` and injected via constructor
- `UploadManager` is `@Singleton` and injected into `MainActivity`

### User / Auth Flow

1. `AccountUtils` (singleton, extends `Core:Auth:CredentialManager`) stores the current `User` in a Room DB
2. On app start `MainViewModel.initCurrentUser()` fetches the current user; `userState` derives from `AccountUtils.getCurrentUserFlow()`
3. If not logged in → `OnboardingScreen` (cross-app login or web login via `InfomaniakLogin`)
4. After login → token and language are injected as cookies; the WebView loads `EURIA_MAIN_URL`
5. Logout clears cookies, `WebStorage`, and removes the user from the DB

### File Upload

`UploadManager` handles both camera captures (JPEG bitmaps) and file picker URIs:
1. Retrieves `organizationId` via `getCurrentOrganizationId()` JS call
2. Calls `prepareFilesForUpload(...)` JS to get the list of accepted file IDs
3. Uploads via `POST /api/1/accounts/{organizationId}/files` (multipart) with a 1-minute timeout
4. Reports success/failure back to the web app via `fileUploadDone` / `fileUploadError` JS calls
5. Parallel uploads are capped at **2 concurrent** uploads (via `Semaphore(2)`)

### Theming

- `EuriaTheme` wraps Material 3, supports light/dark/medium-contrast/high-contrast color schemes
- Custom colors are defined in `CustomColorLight` / `CustomColorDark` and accessed via `LocalCustomColorScheme`
- Dynamic colors are **disabled** (`dynamicColor = false`)

### Build Flavors

| Flavor | Description |
|--------|-------------|
| `standard` | Google Play — includes Firebase push notifications (`Core:Notifications`) |
| `fdroid` | F-Droid — no proprietary dependencies |

The active `ApiEnvironment` is selected at runtime: `PreProd` for debug builds, `Prod` for release builds (see `MainApplication`).

## Key Integration Points (Core modules used)

| Core Module | Usage |
|-------------|-------|
| `Core:Auth` | OAuth2 / token management, `CredentialManager`, `TokenAuthenticator` |
| `Core:Network` | `NetworkConfiguration.init()`, `NetworkAvailability`, OkHttp client |
| `Core:Common` | Extensions, `AssociatedUserDataCleanable`, `SharedValues` |
| `Core:Ui:Compose` | `WebView` composable, `CallableState`, bottom-sheet theme |
| `Core:Ui:View` | `toDp` utility |
| `Core:CrossAppLogin:Back/Front` | Cross-app account handoff (service + UI) |
| `Core:TwoFactorAuth:Back/Front` | 2FA approval bottom sheet |
| `Core:Onboarding` | Onboarding screen wrapper |
| `Core:Sentry` | `SentryLog`, `configureSentry` |
| `Core:Matomo` | Analytics base class |
| `Core:InAppReview` | In-app review flow |
| `Core:Webview` | `WebView` Compose component |
| `Core:Notifications:Registration` | Push notification token registration (standard flavor only) |
| `Core:SharedValues` | `SharedValues` interface for `SharedPreferences`-backed settings |

## Quick Find Commands

```bash
# Find ViewModels
rg -n "class.*ViewModel" app/src/

# Find Hilt entry points
rg -n "@AndroidEntryPoint|@HiltViewModel" app/src/

# Find JavaScript bridge methods
rg -n "@JavascriptInterface" app/src/

# Find Compose screens
rg -n "@Composable" app/src/ -l

# Find all API routes
rg -n "ApiRoutes" app/src/

# Find strings resources
rg -n "R\.string\." app/src/main/

# Find WorkManager workers
rg -n "class.*Worker" app/src/
```

## Definition of Done

- [ ] Code compiles without warnings: `./gradlew :app:compileStandardDebugKotlin`
- [ ] Unit tests pass: `./gradlew :app:test`
- [ ] No hardcoded strings in UI — use `strings.xml` / string resources
- [ ] All new UI built with Jetpack Compose; no new XML layouts
- [ ] New features follow MVVM — logic lives in `ViewModel`, not in `Activity` or `@Composable` functions
- [ ] Hilt DI used for all injectable dependencies
- [ ] Background work uses `WorkManager` or coroutines launched from a `ViewModel`/`applicationScope`
- [ ] Native↔Web interactions go through `JavascriptBridge` and documented JS function contracts
- [ ] No secrets or tokens committed (verify `env.properties` is gitignored)
