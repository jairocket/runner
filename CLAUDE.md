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
./run_tests.sh                                                             # all unit tests
./run_tests.sh --tests "com.runner.ui.tracking.LocationViewModelTest"      # single class
```

The script switches to Java 21 via sdkman (`21.0.6-tem`) and stops any running Gradle daemon before running tests, ensuring Robolectric picks up the correct JVM. Running `./gradlew testDebugUnitTest` directly will fail if the active Java version is 25 or higher.

## Architecture

This is an Android app (Kotlin, min SDK 24, target SDK 36, Java 21) that tracks the user's GPS location in real time. It uses a single-Activity / multi-Fragment structure with the AndroidX Navigation Component and a `BottomNavigationView` with three top-level destinations: **Tracking**, **Map**, and **History**.

**Data flow:**
1. `MainActivity` holds a `FusedLocationProviderClient` and requests location updates every 5 seconds (high-accuracy GPS).
2. Each location fix is pushed into `LocationViewModel` (shared `MutableLiveData<Location>`).
3. `TrackingFragment` observes `LocationViewModel` and renders live metrics (distance, pace, speed, elapsed time) via View Binding.
4. `MapFragment` observes the same ViewModel and draws the active route as an osmdroid `Polyline` overlay; clears it when `trajectorySaved` fires.
5. `HistoryFragment` displays past runs in a `RecyclerView`; tapping a row navigates to `RunDetailFragment`.

**LocationViewModel state** (`ui/tracking/LocateViewModel.kt`):
- `locationLiveData` — latest GPS fix
- `locationHistory` — list of fixes recorded during the active run
- `isTracking` — whether a run is in progress
- `elapsedSeconds`, `distanceKm`, `paceSecPerKm`, `speedKmh` — live run metrics
- `trajectorySaved` — one-shot event that tells `MapFragment` to clear the polyline

**Movement filtering:** fixes with GPS accuracy worse than 20 m are discarded. A displacement segment is only counted as real movement when it exceeds both 0.5 m/s and the GPS accuracy radius, preventing stationary noise from inflating distance.

**History layer** (`ui/history/`):
- `RunRepository` — interface with `getAll()` and `getById(id)`
- `MockRunRepository` — in-memory implementation backed by `MockRuns.kt` (no persistence yet)
- `HistoryViewModel` — exposes `runs: List<RunActivity>` via the repository
- `RunDetailViewModel` — loads a single `RunActivity` by ID; accepts a `RunRepository` for testing
- `RunActivity` — data class: id, date, duration, distanceKm, paceMinKm, `List<Position>`
- `Position` — lat/lon pair; implements `Parcelable` manually (kotlin-parcelize is incompatible with AGP 9.0.0-alpha06)

**Key files:**
- `MainActivity.kt` — permission handling, `FusedLocationProviderClient` lifecycle (starts on resume, stops on pause)
- `ui/tracking/LocateViewModel.kt` — single source of truth for live location and run state
- `ui/tracking/TrackingFragment.kt` — primary UI; start/stop/resume controls and live metrics
- `ui/map/MapFragment.kt` — osmdroid map; draws route polyline, centers on current location
- `ui/history/HistoryFragment.kt` — past runs list; navigates to `RunDetailFragment` on tap
- `ui/history/RunDetailFragment.kt` — displays stats and route replay on an osmdroid map for a past run

**Dependencies** are centralized in `gradle/libs.versions.toml` (version catalog). Key libraries: `play-services-location:21.3.0`, Navigation KTX 2.6.0, Material 1.10.0, osmdroid 6.1.18, RecyclerView 1.3.2.

View Binding is enabled; Jetpack Compose is **not** used.

## Workflow

Always use the `superpowers:test-driven-development` skill before writing any implementation code.

Before finishing a feature branch (before push/PR), install the debug APK on the paired device and smoke test the feature:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Wait for the user to confirm the smoke test passes before proceeding to push.
