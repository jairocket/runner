# Integration Tests

This CLAUDE.md applies only to files under `app/src/androidTest/` — Espresso instrumented tests. It is not loaded for unit tests (`app/src/test/`) or app source code.

## Running integration tests

```bash
# All integration tests
./gradlew connectedAndroidTest

# Single class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.runner.MainActivityTest
```

## Required: disable animations before every test run

Device animations must be off or Espresso tests will flake (views mid-transition fail the visible+90%-area constraint):

```bash
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
```

To restore animations after testing:

```bash
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
```

## Test structure

| File                              | Covers                                 |
|-----------------------------------|----------------------------------------|
| `MainActivityTest.kt`             | Activity launch, bottom nav switching  |
| `ui/tracking/TrackingFlowTest.kt` | Start / Stop / Resume / Save lifecycle |

All tests use `ActivityScenarioRule<MainActivity>` + `GrantPermissionRule` to pre-grant location permission. Rules are declared with `GrantPermissionRule` first so it is applied before the Activity launches.

## Timer-tick dependency

`TrackingFragment.applyTrackingState` only enters STOPPED state when `elapsedSeconds > 0`. Tests that need STOPPED state include a `Thread.sleep(1100)` between Start and Stop to guarantee the 1-second timer has ticked at least once.

## Dependencies

`espresso-contrib` (`androidx.test.espresso:espresso-contrib`) is required for `RecyclerViewActions`. Its version is pinned to `espressoCore` in `gradle/libs.versions.toml` to avoid conflicts. `androidx.test:rules` provides `GrantPermissionRule`.
