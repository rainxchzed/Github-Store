# CLAUDE.md - GitHub Store

## Project Overview

GitHub Store is a cross-platform app store for GitHub releases, built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. Targets **Android** (min API 26) and **Desktop** (Windows, macOS, Linux via JVM).

Package: `zed.rainxch.githubstore`

## Build & Run Commands

```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleRelease

# Desktop (run in dev mode)
./gradlew :composeApp:run

# Desktop installers
./gradlew :composeApp:packageExe :composeApp:packageMsi   # Windows
./gradlew :composeApp:packageDmg :composeApp:packagePkg   # macOS
./gradlew :composeApp:packageDeb :composeApp:packageRpm   # Linux

# Full build check
./gradlew build
```

**Requirements:** JDK 21+ (Temurin recommended), Android SDK for Android builds.

## Project Structure

```
composeApp/                          # Main app module
  src/commonMain/                    # Shared UI & app wiring
  src/androidMain/                   # Android entry point (MainActivity)
  src/jvmMain/                       # Desktop entry point (DesktopApp.kt)
core/
  domain/                            # Shared interfaces, models, utils
  data/                              # Shared repos, networking, database, DI
  presentation/                      # Shared theming & UI utilities
feature/
  {apps,auth,details,dev-profile,    # Each feature has 3 sub-modules:
   favourites,home,search,             domain/ - interfaces & models
   settings,starred}/                  data/   - implementations & DI
                                       presentation/ - screens & ViewModels
build-logic/convention/              # Custom Gradle convention plugins
```

## Architecture

**Clean Architecture + MVVM** with strict layer separation per feature module:

- **Domain** - Repository interfaces, models, use cases (no framework dependencies)
- **Data** - Repository implementations, Ktor API clients, Room DAOs, DTOs
- **Presentation** - ViewModels with `StateFlow`/`Channel`, Compose screens

### State Management Pattern

```kotlin
class XViewModel : ViewModel() {
    private val _state = MutableStateFlow(XState())
    val state = _state.asStateFlow()

    private val _events = Channel<XEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: XAction) { ... }
}
```

Each screen uses a `State` data class, sealed `Action` class for user input, and sealed `Event` class for one-off effects.

### Navigation

Type-safe navigation using `@Serializable` sealed interface `GithubStoreGraph` in `composeApp/src/commonMain/.../app/navigation/`.

### Dependency Injection

**Koin** - modules defined in each feature's `data/di/` directory, registered in `composeApp/.../app/di/initKoin.kt`. ViewModels injected via `koinViewModel()`.

## Tech Stack

| Area | Library | Version |
|------|---------|---------|
| Language | Kotlin | 2.3.0 |
| UI | Compose Multiplatform | 1.9.0-beta01 |
| HTTP | Ktor | 3.2.3 |
| Database | Room | 2.7.2 |
| DI | Koin | 4.1.0 |
| Serialization | Kotlinx Serialization | 1.9.0 |
| Preferences | DataStore | 1.1.7 |
| Image Loading | Landscapist (Coil3) | 2.9.1 |
| Logging | Kermit | 2.0.8 |
| Permissions | MOKO Permissions | 0.19.1 |
| Navigation | Navigation Compose | 2.9.1 |
| Markdown | Multiplatform Markdown Renderer | 0.39.1 |

All versions managed in `gradle/libs.versions.toml` (Version Catalog).

## Convention Plugins

Custom Gradle plugins in `build-logic/convention/` standardize module setup:

| Plugin | Use For |
|--------|---------|
| `convention.kmp.library` | KMP shared library modules |
| `convention.cmp.library` | Compose Multiplatform library modules |
| `convention.cmp.feature` | Feature presentation modules |
| `convention.cmp.application` | Main app module |
| `convention.room` | Room database modules |
| `convention.buildkonfig` | Build-time config (API keys) |

## Adding a New Feature

1. Create `feature/<name>/domain/`, `feature/<name>/data/`, `feature/<name>/presentation/`
2. Add `build.gradle.kts` in each using the appropriate convention plugin
3. Add `include` entries in `settings.gradle.kts`
4. Define domain interfaces/models in `domain/`
5. Implement repository + Koin DI module in `data/di/`
6. Create ViewModel (State/Action/Event pattern) and Screen in `presentation/`
7. Add navigation route to `GithubStoreGraph.kt` and wire in `AppNavigation.kt`
8. Register the Koin module in `initKoin.kt`

## Key Configuration

- **GitHub OAuth:** Set `GITHUB_CLIENT_ID` in `local.properties`. Callback URL: `githubstore://callback`
- **Gradle properties:** Config cache enabled, build cache enabled, 4GB Gradle heap, 3GB Kotlin daemon heap
- **Code style:** Official Kotlin style (`kotlin.code.style=official`)

## Coding Conventions

- Packages follow `zed.rainxch.{module}.{layer}` pattern
- Private state properties use underscore prefix: `_state`
- Sealed classes for type-safe navigation routes, actions, events
- Repository pattern: interface in `domain/`, implementation in `data/`
- Composition over inheritance via Koin DI
- Source sets: `commonMain` for shared, `androidMain` for Android, `jvmMain` for Desktop
