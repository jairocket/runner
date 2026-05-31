# Position-Derived Speed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace GPS Doppler speed (`location.speed`) with speed derived from consecutive position fixes, so that stationary GPS noise never produces a non-zero speed reading.

**Architecture:** `LocationViewModel.updateLocation()` already computes `deltaM / timeDeltaS` for the distance drift filter. Speed is derived from the same calculation: when a position pair passes the drift filter (implied ≥ 0.3 m/s), speed is set to `(deltaM / timeDeltaS * 3.6).toFloat()`; when it is filtered out, speed is explicitly set to `null`. `location.speed` (Doppler) is no longer read.

**Tech Stack:** Kotlin, AndroidX ViewModel, LiveData, Robolectric (unit tests via `./run_tests.sh`)

---

## File Map

| File | What changes |
|------|-------------|
| `app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt` | Remove diagnostic log + import; remove Doppler speed line; derive speed from position delta inside `lastLocation?.let` block |
| `app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt` | Remove 2 Doppler-threshold tests; rewrite 4 tests to use two-location setups; no new files |

---

## Task 1: Remove Diagnostic Logging

The `Log.d("GPS_DIAG", …)` call and its import were added during investigation. Remove them before the implementation change so the commit history is clean.

**Files:**
- Modify: `app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt:7,91`

- [ ] **Step 1: Remove the import and log line**

  In `LocateViewModel.kt`, delete line 7:
  ```kotlin
  // DELETE this line:
  import android.util.Log
  ```
  And delete line 91 (the `Log.d` call inside `updateLocation`):
  ```kotlin
  // DELETE this line:
  Log.d("GPS_DIAG", "hasSpeed=${location.hasSpeed()} rawSpeed=${location.speed} m/s (${location.speed * 3.6f} km/h) accuracy=${location.accuracy}m")
  ```

- [ ] **Step 2: Verify tests still pass**

  ```bash
  ./run_tests.sh --tests "com.runner.ui.tracking.LocationViewModelTest"
  ```
  Expected: `BUILD SUCCESSFUL`, 33 tests, 0 failures.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt
  git commit -m "chore: remove GPS diagnostic logging"
  ```

---

## Task 2: Rewrite Speed Tests to Reflect New Behaviour (RED)

The current speed tests rely on `location.speed` (the Doppler field). After the implementation change those tests will break unpredictably. Rewrite them **before** touching the implementation so every failure is intentional and traceable.

**Files:**
- Modify: `app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt`

Four operations:

### 2a — Delete Doppler-threshold tests (lines 289–309)

These two tests (`updateLocation_withSpeedBelowRunningThreshold_speedKmhIsNull` and `updateLocation_withSpeedAtRunningThreshold_speedKmhIsShown`) were written during an earlier attempt to fix the issue using a Doppler threshold. That approach is being abandoned entirely. Delete both.

- [ ] **Step 1: Delete both tests**

  Remove the following block from the test file (lines 289–309):
  ```kotlin
  @Test
  fun updateLocation_withSpeedBelowRunningThreshold_speedKmhIsNull() {
      viewModel.startTracking()
      val location = Location("test").apply {
          latitude = 0.0; longitude = 0.0
          speed = 0.3f // 0.3 m/s ≈ 1.1 km/h — GPS noise, not real movement
      }
      viewModel.updateLocation(location)
      assertNull(viewModel.speedKmh.value)
  }

  @Test
  fun updateLocation_withSpeedAtRunningThreshold_speedKmhIsShown() {
      viewModel.startTracking()
      val location = Location("test").apply {
          latitude = 0.0; longitude = 0.0
          speed = 0.5f // 0.5 m/s = 1.8 km/h — minimum meaningful speed
      }
      viewModel.updateLocation(location)
      assertNotNull(viewModel.speedKmh.value)
  }
  ```

### 2b — Rewrite `updateLocation_withGpsSpeed_convertsToKmh`

This test uses a single location with `speed = 10.0f`. After the fix, speed requires two locations with a time delta. Rename and rewrite it.

- [ ] **Step 2: Replace the test body**

  Replace:
  ```kotlin
  @Test
  fun updateLocation_withGpsSpeed_convertsToKmh() {
      viewModel.startTracking()
      val location = Location("test").apply {
          latitude = 0.0; longitude = 0.0
          speed = 10.0f // 10 m/s = 36 km/h
      }
      viewModel.updateLocation(location)
      assertEquals(36.0f, viewModel.speedKmh.value!!, 0.01f)
  }
  ```
  With:
  ```kotlin
  @Test
  fun updateLocation_withRealMovement_derivesSpeedFromPosition() {
      viewModel.startTracking()
      // ~11.1 m north in 5 s = 2.22 m/s = 7.99 km/h
      viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L })
      viewModel.updateLocation(Location("test").apply { latitude = 0.0001; longitude = 0.0; time = 5000L })
      assertEquals(8.0f, viewModel.speedKmh.value!!, 0.2f)
  }
  ```

### 2c — Rewrite `updateLocation_whenTracking_setsSpeedKmh`

This test also relies on `location.speed`. Replace it with a drift test that proves the velocity-based filter is what suppresses speed (not `hasSpeed()` returning false).

- [ ] **Step 3: Replace the test body**

  Replace:
  ```kotlin
  @Test
  fun updateLocation_whenTracking_setsSpeedKmh() {
      viewModel.startTracking()
      val location = Location("test").apply {
          latitude = 0.0; longitude = 0.0
          speed = 10.0f // 10 m/s = 36 km/h
      }
      viewModel.updateLocation(location)
      assertEquals(36.0f, viewModel.speedKmh.value!!, 0.01f)
  }
  ```
  With:
  ```kotlin
  @Test
  fun updateLocation_withGpsDrift_speedKmhIsNull() {
      viewModel.startTracking()
      val loc1 = Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L }
      // ~1 m north in 5 s = 0.2 m/s implied; speed field set so hasSpeed()=true,
      // proving it is the velocity filter — not hasSpeed() — that produces null.
      val loc2 = Location("test").apply {
          latitude = 0.000009; longitude = 0.0; time = 5000L
          speed = 0.6f
      }
      viewModel.updateLocation(loc1)
      viewModel.updateLocation(loc2)
      assertNull(viewModel.speedKmh.value)
  }
  ```

### 2d — Rewrite `stopTracking_resetsSpeedKmhToNull` and `resetTimer_resetsSpeedKmhToNull`

Both currently use a single-location setup with `speed = 5.0f`. After the fix, speed is only non-null after a two-location real-movement sequence.

- [ ] **Step 4: Replace both test bodies**

  Replace:
  ```kotlin
  @Test
  fun stopTracking_resetsSpeedKmhToNull() {
      viewModel.startTracking()
      viewModel.updateLocation(Location("test").apply {
          latitude = 0.0; longitude = 0.0; speed = 5.0f
      })
      assertNotNull(viewModel.speedKmh.value)
      viewModel.stopTracking()
      assertNull(viewModel.speedKmh.value)
  }
  ```
  With:
  ```kotlin
  @Test
  fun stopTracking_resetsSpeedKmhToNull() {
      viewModel.startTracking()
      viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L })
      viewModel.updateLocation(Location("test").apply { latitude = 0.0001; longitude = 0.0; time = 5000L })
      assertNotNull(viewModel.speedKmh.value)
      viewModel.stopTracking()
      assertNull(viewModel.speedKmh.value)
  }
  ```

  Replace:
  ```kotlin
  @Test
  fun resetTimer_resetsSpeedKmhToNull() {
      viewModel.startTracking()
      viewModel.updateLocation(Location("test").apply {
          latitude = 0.0; longitude = 0.0; speed = 5.0f
      })
      assertNotNull(viewModel.speedKmh.value)
      viewModel.stopTracking()
      viewModel.resetTimer()
      assertNull(viewModel.speedKmh.value)
  }
  ```
  With:
  ```kotlin
  @Test
  fun resetTimer_resetsSpeedKmhToNull() {
      viewModel.startTracking()
      viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L })
      viewModel.updateLocation(Location("test").apply { latitude = 0.0001; longitude = 0.0; time = 5000L })
      assertNotNull(viewModel.speedKmh.value)
      viewModel.stopTracking()
      viewModel.resetTimer()
      assertNull(viewModel.speedKmh.value)
  }
  ```

- [ ] **Step 5: Verify 4 tests now fail (RED)**

  ```bash
  ./run_tests.sh --tests "com.runner.ui.tracking.LocationViewModelTest"
  ```
  Expected: 4 failures:
  - `updateLocation_withRealMovement_derivesSpeedFromPosition` — current impl returns null (no prev location), expects 8.0f
  - `updateLocation_withGpsDrift_speedKmhIsNull` — current impl: `hasSpeed()=true, speed=0.6>=0.5` → 2.16f, expects null
  - `stopTracking_resetsSpeedKmhToNull` — current impl: two locations, loc2 has no speed set → hasSpeed()=false → null, `assertNotNull` fails
  - `resetTimer_resetsSpeedKmhToNull` — same reason as above

- [ ] **Step 6: Commit the failing tests**

  ```bash
  git add app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt
  git commit -m "test: rewrite speed tests for position-derived speed"
  ```

---

## Task 3: Implement Position-Derived Speed (GREEN)

Replace the Doppler speed line with speed derived from the same `deltaM / timeDeltaS` calculation already used for the distance drift filter.

**Files:**
- Modify: `app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt:89-110`

- [ ] **Step 1: Rewrite `updateLocation`**

  Replace the entire `updateLocation` function body:

  ```kotlin
  fun updateLocation(location: Location) {
      locationLiveData.value = location
      if (_isTracking.value == true) {
          _locationHistory.add(location)
          lastLocation?.let { prev ->
              val deltaM = prev.distanceTo(location)
              val timeDeltaS = (location.time - prev.time) / 1000.0
              val isRealMovement = if (timeDeltaS > 0) deltaM / timeDeltaS >= 0.3 else deltaM > 0f
              if (isRealMovement) {
                  _speedKmh.value = if (timeDeltaS > 0) (deltaM / timeDeltaS * 3.6).toFloat() else null
                  val newDist = (_distanceKm.value ?: 0.0) + deltaM / 1000.0
                  _distanceKm.value = newDist
                  val secs = _elapsedSeconds.value ?: 0L
                  if (newDist >= 0.01 && secs > 0) {
                      _paceSecPerKm.value = secs.toDouble() / newDist
                  }
              } else {
                  _speedKmh.value = null
              }
          }
          lastLocation = location
      }
  }
  ```

  Key changes from previous version:
  - The Doppler line `_speedKmh.value = if (location.hasSpeed() && …)` is **gone**
  - When `isRealMovement`: speed = `(deltaM / timeDeltaS * 3.6).toFloat()`
  - When drift detected (`!isRealMovement`): speed = `null` (explicit, clears any previous value)
  - When `lastLocation == null` (first fix): the `?.let` does not run; speed stays `null` (set by `startTracking`)
  - `timeDeltaS <= 0` fallback: speed = `null` (conservative; in production `location.time` is always populated)

- [ ] **Step 2: Verify all tests pass (GREEN)**

  ```bash
  ./run_tests.sh --tests "com.runner.ui.tracking.LocationViewModelTest"
  ```
  Expected: `BUILD SUCCESSFUL`, 31 tests, 0 failures.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt
  git commit -m "fix: derive speed from position delta instead of GPS Doppler field"
  ```

---

## Task 4: Install and Smoke Test

- [ ] **Step 1: Build and install**

  ```bash
  ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

- [ ] **Step 2: Stationary test**

  Open the app, press **Start**, leave the phone on the table for 60 seconds. Verify:
  - Speed shows `--` the entire time
  - Distance stays at `0.00`
  - Pace shows `--:--`

- [ ] **Step 3: Movement test**

  Walk briskly with the phone for 30 seconds. Verify:
  - Speed shows a plausible value (4–8 km/h for walking)
  - Distance accumulates
  - Pace appears after the first real movement pair is recorded (after ~5–10 s of walking)

  Confirm with the user before proceeding.
