# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build                  # Build debug + release APKs
./gradlew assembleDebug          # Build debug APK only
./gradlew connectedAndroidTest   # Run instrumented tests on a device/emulator
./gradlew clean                  # Clean build artifacts
./gradlew lint                   # Run Android lint checks
```

### Running unit tests

Unit tests use Robolectric 4.11.1, which requires **Java 21**. The system default may be a different version. Always use `run_tests.sh` instead of calling Gradle directly:

```bash
./run_tests.sh                                                                    # all unit tests
./run_tests.sh --tests "com.example.runner.ui.tracking.LocationViewModelTest"     # single class
```

The script switches to Java 21 via sdkman (`21.0.6-tem`) and stops any running Gradle daemon before running tests, ensuring Robolectric picks up the correct JVM. Running `./gradlew testDebugUnitTest` directly will fail if the active Java version is 25 or higher.

## Architecture

This is an Android app (Kotlin, min SDK 24, target SDK 36, Java 21) that tracks the user's GPS location in real time. It uses a single-Activity / multi-Fragment structure with the AndroidX Navigation Component.

**Data flow:**
1. `MainActivity` holds a `FusedLocationProviderClient` and requests location updates every 5 seconds (high-accuracy GPS).
2. Each location fix is pushed into `LocationViewModel` (shared `MutableLiveData<Location>`).
3. `FirstFragment` observes `LocationViewModel` and renders latitude, longitude, speed, and timestamp via View Binding.
4. `SecondFragment` is a secondary destination reachable from `FirstFragment`; navigation is defined in `res/navigation/nav_graph.xml`.

**Key files:**
- `MainActivity.kt` — permission handling, `FusedLocationProviderClient` lifecycle (starts on resume, stops on pause)
- `ui/tracking/LocationViewModel.kt` — single source of truth for live location data
- `FirstFragment.kt` — primary UI; observes the ViewModel
- `SecondFragment.kt` — secondary screen, no location logic

**Dependencies** are centralized in `gradle/libs.versions.toml` (version catalog). Key libraries: `play-services-location:21.3.0`, Navigation KTX 2.6.0, Material 1.10.0.

View Binding is enabled; Jetpack Compose is **not** used.

## Workflow

Always use the `superpowers:test-driven-development` skill before writing any implementation code.
