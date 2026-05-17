# Clear Map Trajectory on Save — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When the user taps Save in TrackingFragment, the drawn route polyline on MapFragment is cleared.

**Architecture:** `LocationViewModel.resetTimer()` is extended to clear `locationHistory` and emit a `trajectorySaved` LiveData event. `MapFragment` observes the event and clears its `routePolyline`. No changes to `TrackingFragment`.

**Tech Stack:** Kotlin, AndroidX LiveData, OsmDroid Polyline, Robolectric 4.11.1 (unit tests via `./run_tests.sh`)

---

## File Map

| File | Change |
|------|--------|
| `app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt` | Add `trajectorySaved` LiveData; extend `resetTimer()` to clear history and emit event |
| `app/src/main/java/com/runner/ui/map/MapFragment.kt` | Make `routePolyline` internal; observe `trajectorySaved` and clear polyline |
| `app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt` | Add two tests for new `resetTimer()` behaviour |
| `app/src/test/java/com/runner/ui/map/MapFragmentTest.kt` | Add one test verifying polyline is cleared on save |

---

## Task 1: Extend `resetTimer()` in `LocationViewModel`

**Files:**
- Modify: `app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt`
- Test: `app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt`

- [ ] **Step 1: Write two failing tests**

Add both tests at the end of the `// ── resetTimer ──` block in `LocationViewModelTest.kt`:

```kotlin
@Test
fun resetTimer_clearsLocationHistory() {
    viewModel.startTracking()
    viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
    viewModel.stopTracking()
    viewModel.resetTimer()
    assertTrue(viewModel.locationHistory.isEmpty())
}

@Test
fun resetTimer_emitsTrajectorySaved() {
    viewModel.startTracking()
    viewModel.stopTracking()
    var emitted = false
    viewModel.trajectorySaved.observeForever { emitted = true }
    viewModel.resetTimer()
    assertTrue(emitted)
}
```

- [ ] **Step 2: Run tests and verify they fail**

```bash
./run_tests.sh --tests "com.runner.ui.tracking.LocationViewModelTest"
```

Expected: both new tests FAIL — `trajectorySaved` does not exist yet and `locationHistory` is not cleared.

- [ ] **Step 3: Implement the changes in `LocateViewModel.kt`**

Add the LiveData property after the existing `paceSecPerKm` declarations:

```kotlin
private val _trajectorySaved = MutableLiveData<Unit>()
val trajectorySaved: LiveData<Unit> = _trajectorySaved
```

Replace the existing `resetTimer()` body:

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

- [ ] **Step 4: Run tests and verify they pass**

```bash
./run_tests.sh --tests "com.runner.ui.tracking.LocationViewModelTest"
```

Expected: all tests PASS, including the two new ones.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/runner/ui/tracking/LocateViewModel.kt \
        app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt
git commit -m "feat: emit trajectorySaved and clear locationHistory on resetTimer"
```

---

## Task 2: Observe `trajectorySaved` in `MapFragment` and clear polyline

**Files:**
- Modify: `app/src/main/java/com/runner/ui/map/MapFragment.kt`
- Test: `app/src/test/java/com/runner/ui/map/MapFragmentTest.kt`

- [ ] **Step 1: Write a failing test**

Add to `MapFragmentTest.kt`:

```kotlin
@Test
fun trajectorySaved_clearsRoutePolyline() {
    launch().onFragment { fragment ->
        val viewModel = ViewModelProvider(fragment.requireActivity())[LocationViewModel::class.java]
        viewModel.startTracking()
        viewModel.updateLocation(Location("test").apply {
            latitude = 48.8566
            longitude = 2.3522
        })
        viewModel.stopTracking()
        viewModel.resetTimer()
        assertTrue(fragment.routePolyline.points.isEmpty())
    }
}
```

This test also needs the `Location` import — add at the top of `MapFragmentTest.kt` if not already present:

```kotlin
import android.location.Location
```

- [ ] **Step 2: Run test and verify it fails**

```bash
./run_tests.sh --tests "com.runner.ui.map.MapFragmentTest"
```

Expected: FAIL — `routePolyline` is private so the test won't compile.

- [ ] **Step 3: Implement the changes in `MapFragment.kt`**

Change `routePolyline` from `private` to `internal`:

```kotlin
internal val routePolyline = Polyline()
```

Add the observer at the end of `onViewCreated`, after the existing `locationLiveData` observer block:

```kotlin
viewModel.trajectorySaved.observe(viewLifecycleOwner) {
    routePolyline.setPoints(emptyList())
    binding.mapView.invalidate()
}
```

- [ ] **Step 4: Run tests and verify they pass**

```bash
./run_tests.sh --tests "com.runner.ui.map.MapFragmentTest"
```

Expected: all tests PASS.

- [ ] **Step 5: Run the full test suite**

```bash
./run_tests.sh
```

Expected: all tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/runner/ui/map/MapFragment.kt \
        app/src/test/java/com/runner/ui/map/MapFragmentTest.kt
git commit -m "feat: clear map polyline when run is saved"
```
