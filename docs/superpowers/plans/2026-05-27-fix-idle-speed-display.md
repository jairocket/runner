# Fix: Instant Speed Shown While Idle

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Instant speed (`speedKmh`) must only show a value while a run is actively in progress; it must be null in IDLE and STOPPED states.

**Architecture:** The fix lives entirely in `LocationViewModel`. `_speedKmh` is currently set unconditionally in `updateLocation()`, bypassing the `isTracking` guard. Moving it inside that guard, and nulling it out on `stopTracking()` / `startTracking()` / `resetTimer()`, makes the ViewModel the single source of truth — the Fragment requires no changes.

**Tech Stack:** Kotlin, AndroidX LiveData, Robolectric 4.11.1 (Java 21 via `./run_tests.sh`)

---

## Files

| Action | Path |
|--------|------|
| Modify | `app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt` |
| Modify | `app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt` |

---

### Task 1: Add failing tests for idle-speed bug

**Files:**
- Modify: `app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt`

- [ ] **Step 1: Add four new tests inside the `// ── speedKmh ──` section (after line 242)**

  The four cases that must hold after the fix:

  ```kotlin
  @Test
  fun updateLocation_whenNotTracking_doesNotSetSpeedKmh() {
      val location = Location("test").apply {
          latitude = 0.0; longitude = 0.0
          speed = 10.0f
      }
      viewModel.updateLocation(location)
      assertNull(viewModel.speedKmh.value)
  }

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

  @Test
  fun stopTracking_resetsSpeedKmhToNull() {
      viewModel.startTracking()
      viewModel.updateLocation(Location("test").apply {
          latitude = 0.0; longitude = 0.0; speed = 5.0f
      })
      viewModel.stopTracking()
      assertNull(viewModel.speedKmh.value)
  }

  @Test
  fun resetTimer_resetsSpeedKmhToNull() {
      viewModel.startTracking()
      viewModel.updateLocation(Location("test").apply {
          latitude = 0.0; longitude = 0.0; speed = 5.0f
      })
      viewModel.stopTracking()
      viewModel.resetTimer()
      assertNull(viewModel.speedKmh.value)
  }
  ```

- [ ] **Step 2: Also update the existing `updateLocation_withGpsSpeed_convertsToKmh` test (line 228)**

  It currently calls `updateLocation()` without starting tracking. After the fix it will fail. Replace it:

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

- [ ] **Step 3: Run the new tests to confirm they fail (and the existing one fails too)**

  ```bash
  ./run_tests.sh --tests "com.runner.ui.tracking.LocationViewModelTest"
  ```

  Expected: `updateLocation_whenNotTracking_doesNotSetSpeedKmh`, `stopTracking_resetsSpeedKmhToNull`, `resetTimer_resetsSpeedKmhToNull`, and the updated `updateLocation_withGpsSpeed_convertsToKmh` all FAIL.

---

### Task 2: Fix `LocateViewModel` — guard speed behind `isTracking`

**Files:**
- Modify: `app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt`

- [ ] **Step 1: Move `_speedKmh` update inside the tracking guard in `updateLocation()`**

  Current (`LocateViewModel.kt:85-103`):
  ```kotlin
  fun updateLocation(location: Location) {
      locationLiveData.value = location
      _speedKmh.value = if (location.hasSpeed()) location.speed * 3.6f else null
      if (_isTracking.value == true) {
          _locationHistory.add(location)
          lastLocation?.let { prev ->
              val deltaM = prev.distanceTo(location)
              if (deltaM > 0f) {
                  val newDist = (_distanceKm.value ?: 0.0) + deltaM / 1000.0
                  _distanceKm.value = newDist
                  val secs = _elapsedSeconds.value ?: 0L
                  if (newDist >= 0.01 && secs > 0) {
                      _paceSecPerKm.value = secs.toDouble() / newDist
                  }
              }
          }
          lastLocation = location
      }
  }
  ```

  Replace with:
  ```kotlin
  fun updateLocation(location: Location) {
      locationLiveData.value = location
      if (_isTracking.value == true) {
          _speedKmh.value = if (location.hasSpeed()) location.speed * 3.6f else null
          _locationHistory.add(location)
          lastLocation?.let { prev ->
              val deltaM = prev.distanceTo(location)
              if (deltaM > 0f) {
                  val newDist = (_distanceKm.value ?: 0.0) + deltaM / 1000.0
                  _distanceKm.value = newDist
                  val secs = _elapsedSeconds.value ?: 0L
                  if (newDist >= 0.01 && secs > 0) {
                      _paceSecPerKm.value = secs.toDouble() / newDist
                  }
              }
          }
          lastLocation = location
      }
  }
  ```

- [ ] **Step 2: Reset `_speedKmh` to null in `stopTracking()`, `startTracking()`, and `resetTimer()`**

  Current `stopTracking()` (`LocateViewModel.kt:59-64`):
  ```kotlin
  fun stopTracking() {
      if (_isTracking.value != true) return
      _isTracking.value = false
      handler.removeCallbacks(timerRunnable)
      lastLocation = null
  }
  ```

  Replace with:
  ```kotlin
  fun stopTracking() {
      if (_isTracking.value != true) return
      _isTracking.value = false
      handler.removeCallbacks(timerRunnable)
      lastLocation = null
      _speedKmh.value = null
  }
  ```

  Current `startTracking()` (`LocateViewModel.kt:47-57`):
  ```kotlin
  fun startTracking() {
      if (_isTracking.value == true) return
      _locationHistory.clear()
      _distanceKm.value = 0.0
      _paceSecPerKm.value = null
      _elapsedSeconds.value = 0L
      lastLocation = null
      trackingStartMs = SystemClock.elapsedRealtime()
      _isTracking.value = true
      handler.post(timerRunnable)
  }
  ```

  Replace with:
  ```kotlin
  fun startTracking() {
      if (_isTracking.value == true) return
      _locationHistory.clear()
      _distanceKm.value = 0.0
      _paceSecPerKm.value = null
      _speedKmh.value = null
      _elapsedSeconds.value = 0L
      lastLocation = null
      trackingStartMs = SystemClock.elapsedRealtime()
      _isTracking.value = true
      handler.post(timerRunnable)
  }
  ```

  Current `resetTimer()` (`LocateViewModel.kt:76-83`):
  ```kotlin
  fun resetTimer() {
      if (_isTracking.value == true) return
      _elapsedSeconds.value = 0L
      _distanceKm.value = 0.0
      _paceSecPerKm.value = null
      _locationHistory.clear()
      _trajectorySaved.value = Unit
  }
  ```

  Replace with:
  ```kotlin
  fun resetTimer() {
      if (_isTracking.value == true) return
      _elapsedSeconds.value = 0L
      _distanceKm.value = 0.0
      _paceSecPerKm.value = null
      _speedKmh.value = null
      _locationHistory.clear()
      _trajectorySaved.value = Unit
  }
  ```

- [ ] **Step 3: Run the full test suite to confirm all tests pass**

  ```bash
  ./run_tests.sh --tests "com.runner.ui.tracking.LocationViewModelTest"
  ```

  Expected: ALL tests PASS (including the 4 new ones).

- [ ] **Step 4: Commit**

  ```bash
  git add app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt \
          app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt
  git commit -m "fix: only show instant speed while tracking, clear on stop/reset"
  ```
