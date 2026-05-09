# Runner

An Android app that tracks the user's GPS location in real time, displaying latitude, longitude, speed, and timestamp.

## Requirements

- Android Studio or the Android SDK command-line tools
- Java 21 (via [sdkman](https://sdkman.io/) — see note below)
- A device or emulator running Android 7.0+ (API 24)

## Building

```bash
./gradlew assembleDebug     # debug APK → app/build/outputs/apk/debug/
./gradlew build             # debug + release APKs
```

## Running tests

Unit tests use Robolectric, which requires **Java 21**. Use the provided script instead of calling Gradle directly:

```bash
./run_tests.sh                                                                 # all unit tests
./run_tests.sh --tests "com.example.runner.ui.tracking.LocationViewModelTest"  # single class
```

The script switches to Java 21 via sdkman and restarts the Gradle daemon under the correct JVM before running tests. Calling `./gradlew testDebugUnitTest` directly will fail if your active Java version is 25 or higher.

For instrumented tests (requires a connected device or emulator):

```bash
./gradlew connectedAndroidTest
```

## Architecture

Single-Activity / multi-Fragment app using the AndroidX Navigation Component.

**Data flow:** `MainActivity` collects GPS fixes every 5 seconds via `FusedLocationProviderClient` → pushes each fix into `LocationViewModel` (shared `MutableLiveData<Location>`) → `TrackingFragment` observes the ViewModel and renders the data.

**Key files:**

| File | Role |
|---|---|
| `MainActivity.kt` | Permission handling, location client lifecycle |
| `ui/tracking/LocationViewModel.kt` | Single source of truth for live location |
| `ui/tracking/TrackingFragment.kt` | Primary UI, observes ViewModel |
| `res/navigation/nav_graph.xml` | Navigation graph |

View Binding is enabled; Jetpack Compose is not used.
