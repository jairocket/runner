# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build                  # Build debug + release APKs
./gradlew assembleDebug          # Build debug APK only
./gradlew test                   # Run unit tests
./gradlew testDebugUnitTest      # Run a single test variant
./gradlew connectedAndroidTest   # Run instrumented tests on a device/emulator
./gradlew clean                  # Clean build artifacts
./gradlew lint                   # Run Android lint checks
```

To run a single test class:
```bash
./gradlew test --tests "com.example.runner.ExampleUnitTest"
```

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
