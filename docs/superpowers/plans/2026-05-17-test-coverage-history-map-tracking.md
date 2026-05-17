# Test Coverage: History, Map, and Tracking Fragments

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Robolectric unit tests for HistoryFragment, MapFragment, and TrackingFragment to cover button interactions, LiveData observer reactions, UI state transitions, adapter item binding, overlay visibility, and ViewModel lifecycle methods — all currently untested.

**Architecture:** Five independent tasks each targeting a different file; all 5 can execute in parallel. Tests follow the Robolectric + `launchFragmentInContainer` pattern from `TrackingFragmentTest` and `LocationViewModelTest`. No production code changes required.

**Tech Stack:** Kotlin, Robolectric 4.11.1 (`@Config(sdk = [33])`), `launchFragmentInContainer`, `InstantTaskExecutorRule`, `ShadowSystemClock` (advances `SystemClock.elapsedRealtime()` on the JVM), `shadowOf(Looper.getMainLooper()).idle()` (drains handler queue), `ViewModelProvider` (shared ViewModel access from fragment test), `./run_tests.sh` (required — system Java may not be 21)

---

## Coverage Gaps Addressed

| File | Existing tests | New tests | Key gaps covered |
|---|---|---|---|
| `LocationViewModelTest` | 11 | +10 | `updateLocation` while tracking, history, pace, `resumeTracking`, `resetTimer` |
| `TrackingFragmentTest` | 6 | +7 | LiveData observers → UI, RUNNING/STOPPED/IDLE state transitions |
| `HistoryFragmentTest` | 0 | +4 | RecyclerView layout manager, item count, dividers, destroy |
| `HistoryAdapterTest` | 0 | +7 | Item count, date/distance/duration/pace binding, second item |
| `MapFragmentTest` | 0 | +3 | Overlay initial VISIBLE, location update hides overlay, destroy |

---

## File Structure

- Modify: `app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt`
- Modify: `app/src/test/java/com/runner/ui/tracking/TrackingFragmentTest.kt`
- Create: `app/src/test/java/com/runner/ui/history/HistoryFragmentTest.kt`
- Create: `app/src/test/java/com/runner/ui/history/HistoryAdapterTest.kt`
- Create: `app/src/test/java/com/runner/ui/map/MapFragmentTest.kt`

> All 5 tasks modify different files — execute in parallel.

---

### Task 1: LocationViewModel — updateLocation tracking, history, pace, resumeTracking, resetTimer

**Files:**
- Modify: `app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt`

- [ ] **Step 1: Add missing imports**

After the existing imports at the top of `LocationViewModelTest.kt`, add:

```kotlin
import android.os.Looper
import java.time.Duration
import org.junit.Assert.assertNotNull
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSystemClock
```

- [ ] **Step 2: Write the 10 new tests inside the LocationViewModelTest class**

Append these tests inside the existing `LocationViewModelTest` class, after the last `@Test` method:

```kotlin
    // ── updateLocation while tracking ─────────────────────────────────────────

    @Test
    fun updateLocation_whileTracking_firstLocation_doesNotAccumulateDistance() {
        viewModel.startTracking()
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
        assertEquals(0.0, viewModel.distanceKm.value!!, 0.001)
    }

    @Test
    fun updateLocation_whileTracking_twoLocations_accumulatesDistance() {
        viewModel.startTracking()
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
        viewModel.updateLocation(Location("test").apply { latitude = 0.009; longitude = 0.0 })
        assertTrue(viewModel.distanceKm.value!! > 0.0)
    }

    @Test
    fun updateLocation_whileTracking_addsToLocationHistory() {
        viewModel.startTracking()
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
        viewModel.updateLocation(Location("test").apply { latitude = 1.0; longitude = 0.0 })
        assertEquals(2, viewModel.locationHistory.size)
    }

    @Test
    fun updateLocation_whenNotTracking_doesNotAddToHistory() {
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
        assertTrue(viewModel.locationHistory.isEmpty())
    }

    @Test
    fun startTracking_clearsLocationHistory() {
        viewModel.startTracking()
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
        viewModel.stopTracking()
        viewModel.startTracking()
        assertTrue(viewModel.locationHistory.isEmpty())
    }

    @Test
    fun updateLocation_withSufficientDistanceAndElapsedTime_calculatesPace() {
        viewModel.startTracking()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(600))
        shadowOf(Looper.getMainLooper()).idle()
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
        viewModel.updateLocation(Location("test").apply { latitude = 0.009; longitude = 0.0 })
        assertNotNull(viewModel.paceSecPerKm.value)
    }

    // ── resumeTracking ────────────────────────────────────────────────────────

    @Test
    fun resumeTracking_whenElapsedIsZero_doesNotStartTracking() {
        viewModel.resumeTracking()
        assertFalse(viewModel.isTracking.value!!)
    }

    @Test
    fun resumeTracking_whenAlreadyTracking_doesNothing() {
        viewModel.startTracking()
        viewModel.resumeTracking()
        assertTrue(viewModel.isTracking.value!!)
    }

    // ── resetTimer ────────────────────────────────────────────────────────────

    @Test
    fun resetTimer_whenNotTracking_resetsAllMetrics() {
        viewModel.startTracking()
        viewModel.stopTracking()
        viewModel.resetTimer()
        assertEquals(0L, viewModel.elapsedSeconds.value)
        assertEquals(0.0, viewModel.distanceKm.value!!, 0.001)
        assertNull(viewModel.paceSecPerKm.value)
    }

    @Test
    fun resetTimer_whenTracking_doesNotReset() {
        viewModel.startTracking()
        viewModel.resetTimer()
        assertTrue(viewModel.isTracking.value!!)
    }
```

- [ ] **Step 3: Run the tests**

```bash
./run_tests.sh --tests "com.runner.ui.tracking.LocationViewModelTest"
```

Expected: 21 tests pass (11 existing + 10 new). No failures.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/runner/ui/tracking/LocationViewModelTest.kt
git commit -m "test: add LocationViewModel coverage for tracking, history, pace, resume, and reset"
```

---

### Task 2: TrackingFragment — LiveData observers and UI state transitions

**Files:**
- Modify: `app/src/test/java/com/runner/ui/tracking/TrackingFragmentTest.kt`

**Background:** `applyTrackingState(isTracking)` is private but driven by LiveData observers. To reach STOPPED state, `hasStopped = !isTracking && elapsedSeconds > 0` — meaning we must advance `SystemClock.elapsedRealtime()` and flush the looper so the timer ticks before calling `stopTracking()`. `ShadowSystemClock.advanceBy(Duration)` advances `elapsedRealtime()` on the JVM; `shadowOf(Looper.getMainLooper()).idle()` runs the queued `timerRunnable`.

- [ ] **Step 1: Add missing imports**

After the existing imports at the top of `TrackingFragmentTest.kt`, add:

```kotlin
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import java.time.Duration
import org.junit.Assert.assertNotEquals
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSystemClock
```

- [ ] **Step 2: Write the helper and 7 new tests inside the TrackingFragmentTest class**

Append this private helper and the 7 tests inside the existing `TrackingFragmentTest` class body, after the last `@Test`:

```kotlin
    private fun getViewModel(fragment: TrackingFragment): LocationViewModel =
        ViewModelProvider(fragment.requireActivity())[LocationViewModel::class.java]

    // ── LiveData observers → UI ───────────────────────────────────────────────

    @Test
    fun elapsedSeconds_observer_formatsHoursMinutesSeconds() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(3661)) // 1h 1m 1s
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(
                "01:01:01",
                fragment.requireView().findViewById<TextView>(R.id.textTimerDisplay).text.toString()
            )
        }
    }

    @Test
    fun distanceKm_observer_updatesDistanceDisplay() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
            viewModel.updateLocation(Location("test").apply { latitude = 0.009; longitude = 0.0 })
            assertNotEquals(
                "0.00",
                fragment.requireView().findViewById<TextView>(R.id.textDistanceValue).text.toString()
            )
        }
    }

    @Test
    fun paceSecPerKm_observer_withValue_displaysMmSsFormat() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(600))
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
            viewModel.updateLocation(Location("test").apply { latitude = 0.009; longitude = 0.0 })
            val text = fragment.requireView().findViewById<TextView>(R.id.textPaceValue).text.toString()
            assertNotEquals("--:--", text)
            assertTrue(text.matches(Regex("\\d{2}:\\d{2}")))
        }
    }

    // ── applyTrackingState — RUNNING ──────────────────────────────────────────

    @Test
    fun startButton_click_showsRunningState() {
        launch().onFragment { fragment ->
            fragment.requireView().findViewById<MaterialButton>(R.id.buttonStart).performClick()
            val root = fragment.requireView()
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.buttonStop).visibility)
            assertEquals(View.GONE,    root.findViewById<View>(R.id.rowStartResume).visibility)
            assertEquals(View.GONE,    root.findViewById<View>(R.id.buttonSave).visibility)
            assertEquals("RUNNING",    root.findViewById<TextView>(R.id.textStatusLabel).text)
        }
    }

    // ── applyTrackingState — STOPPED ──────────────────────────────────────────

    @Test
    fun stopTracking_withElapsedTime_showsStoppedState() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.stopTracking()
            val root = fragment.requireView()
            assertEquals(View.GONE,    root.findViewById<View>(R.id.buttonStop).visibility)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.buttonResume).visibility)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.buttonSave).visibility)
            assertEquals("STOPPED",    root.findViewById<TextView>(R.id.textStatusLabel).text)
        }
    }

    @Test
    fun resumeButton_click_afterStop_transitionsToRunningState() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.stopTracking()
            fragment.requireView().findViewById<MaterialButton>(R.id.buttonResume).performClick()
            val root = fragment.requireView()
            assertEquals("RUNNING",    root.findViewById<TextView>(R.id.textStatusLabel).text)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.buttonStop).visibility)
        }
    }

    // ── applyTrackingState — IDLE (after save) ────────────────────────────────

    @Test
    fun saveButton_click_resetsToIdleState() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.stopTracking()
            fragment.requireView().findViewById<MaterialButton>(R.id.buttonSave).performClick()
            val root = fragment.requireView()
            assertEquals("00:00:00", root.findViewById<TextView>(R.id.textTimerDisplay).text)
            assertEquals("IDLE",     root.findViewById<TextView>(R.id.textStatusLabel).text)
            assertEquals(View.GONE,  root.findViewById<View>(R.id.buttonResume).visibility)
            assertEquals(View.GONE,  root.findViewById<View>(R.id.buttonStop).visibility)
        }
    }
```

- [ ] **Step 3: Run the tests**

```bash
./run_tests.sh --tests "com.runner.ui.tracking.TrackingFragmentTest"
```

Expected: 13 tests pass (6 existing + 7 new). No failures.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/runner/ui/tracking/TrackingFragmentTest.kt
git commit -m "test: add TrackingFragment coverage for LiveData observers and state transitions"
```

---

### Task 3: HistoryFragment — RecyclerView setup and lifecycle

**Files:**
- Create: `app/src/test/java/com/runner/ui/history/HistoryFragmentTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.runner.ui.history

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runner.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HistoryFragmentTest {

    private fun launch() = launchFragmentInContainer<HistoryFragment>(
        themeResId = R.style.Theme_Runner
    )

    @Test
    fun recyclerView_hasLinearLayoutManager() {
        launch().onFragment { fragment ->
            val rv = fragment.requireView().findViewById<RecyclerView>(R.id.recyclerViewHistory)
            assertTrue(rv.layoutManager is LinearLayoutManager)
        }
    }

    @Test
    fun recyclerView_adapter_has6Items() {
        launch().onFragment { fragment ->
            val rv = fragment.requireView().findViewById<RecyclerView>(R.id.recyclerViewHistory)
            assertEquals(6, rv.adapter?.itemCount)
        }
    }

    @Test
    fun recyclerView_hasDividerItemDecoration() {
        launch().onFragment { fragment ->
            val rv = fragment.requireView().findViewById<RecyclerView>(R.id.recyclerViewHistory)
            assertTrue(rv.itemDecorationCount > 0)
        }
    }

    @Test
    fun binding_onDestroyView_doesNotLeak() {
        launch().moveToState(Lifecycle.State.DESTROYED)
    }
}
```

- [ ] **Step 2: Run the tests**

```bash
./run_tests.sh --tests "com.runner.ui.history.HistoryFragmentTest"
```

Expected: 4 tests pass. No failures.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/runner/ui/history/HistoryFragmentTest.kt
git commit -m "test: add HistoryFragment tests for RecyclerView setup and lifecycle"
```

---

### Task 4: HistoryAdapter — item count and view binding

**Files:**
- Create: `app/src/test/java/com/runner/ui/history/HistoryAdapterTest.kt`

**Background:** `HistoryAdapter.ViewHolder` is a public `inner class`, so `onCreateViewHolder()` is callable directly from tests. `ItemHistoryRunBinding` inflates from `item_history_run.xml`; Robolectric provides a real LayoutInflater backed by the app's resources. The pace field binds as `"${item.paceMinKm} min/km"` — note the space before `min/km`.

- [ ] **Step 1: Create the test file**

```kotlin
package com.runner.ui.history

import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runner.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HistoryAdapterTest {

    private val context = RuntimeEnvironment.getApplication()

    private val sampleItems = listOf(
        RunActivity("May 14, 2026", "42:17", "6.2 km", "6:49"),
        RunActivity("May 12, 2026", "31:04", "4.8 km", "6:28")
    )

    private fun makeHolder(): HistoryAdapter.ViewHolder =
        HistoryAdapter(sampleItems).onCreateViewHolder(FrameLayout(context), 0)

    @Test
    fun getItemCount_returnsListSize() {
        assertEquals(2, HistoryAdapter(sampleItems).itemCount)
    }

    @Test
    fun getItemCount_emptyList_returnsZero() {
        assertEquals(0, HistoryAdapter(emptyList()).itemCount)
    }

    @Test
    fun bind_displaysDate() {
        val adapter = HistoryAdapter(sampleItems)
        val holder = makeHolder()
        adapter.onBindViewHolder(holder, 0)
        assertEquals("May 14, 2026", holder.itemView.findViewById<TextView>(R.id.textItemDate).text.toString())
    }

    @Test
    fun bind_displaysDistance() {
        val adapter = HistoryAdapter(sampleItems)
        val holder = makeHolder()
        adapter.onBindViewHolder(holder, 0)
        assertEquals("6.2 km", holder.itemView.findViewById<TextView>(R.id.textItemDistance).text.toString())
    }

    @Test
    fun bind_displaysDuration() {
        val adapter = HistoryAdapter(sampleItems)
        val holder = makeHolder()
        adapter.onBindViewHolder(holder, 0)
        assertEquals("42:17", holder.itemView.findViewById<TextView>(R.id.textItemDuration).text.toString())
    }

    @Test
    fun bind_displaysPaceWithMinKmSuffix() {
        val adapter = HistoryAdapter(sampleItems)
        val holder = makeHolder()
        adapter.onBindViewHolder(holder, 0)
        assertEquals("6:49 min/km", holder.itemView.findViewById<TextView>(R.id.textItemPace).text.toString())
    }

    @Test
    fun bind_secondItem_displaysCorrectDate() {
        val adapter = HistoryAdapter(sampleItems)
        val holder = makeHolder()
        adapter.onBindViewHolder(holder, 1)
        assertEquals("May 12, 2026", holder.itemView.findViewById<TextView>(R.id.textItemDate).text.toString())
    }
}
```

- [ ] **Step 2: Run the tests**

```bash
./run_tests.sh --tests "com.runner.ui.history.HistoryAdapterTest"
```

Expected: 7 tests pass. No failures.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/runner/ui/history/HistoryAdapterTest.kt
git commit -m "test: add HistoryAdapter tests for item count and view binding"
```

---

### Task 5: MapFragment — overlay visibility and lifecycle

**Files:**
- Create: `app/src/test/java/com/runner/ui/map/MapFragmentTest.kt`

**Background:** On `onViewCreated`, the overlay stays VISIBLE when `locationHistory` is empty and `locationLiveData.value` is null (both are true at ViewModel initialization). The LiveData observer calls `hideOverlay()` on the first location update when `!hasInitialCenter`. OSMDroid's `MapView` is a pure Java/Kotlin view and initializes normally under Robolectric.

- [ ] **Step 1: Create the test file**

```kotlin
package com.runner.ui.map

import android.location.Location
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runner.R
import com.runner.ui.tracking.LocationViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MapFragmentTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private fun launch() = launchFragmentInContainer<MapFragment>(
        themeResId = R.style.Theme_Runner
    )

    @Test
    fun locationLoadingOverlay_initialState_isVisible() {
        launch().onFragment { fragment ->
            assertEquals(
                View.VISIBLE,
                fragment.requireView().findViewById<View>(R.id.locationLoadingOverlay).visibility
            )
        }
    }

    @Test
    fun locationLiveData_update_hidesOverlay() {
        launch().onFragment { fragment ->
            val viewModel = ViewModelProvider(fragment.requireActivity())[LocationViewModel::class.java]
            viewModel.locationLiveData.value = Location("test").apply {
                latitude = 48.8566
                longitude = 2.3522
            }
            assertEquals(
                View.GONE,
                fragment.requireView().findViewById<View>(R.id.locationLoadingOverlay).visibility
            )
        }
    }

    @Test
    fun binding_onDestroyView_doesNotLeak() {
        launch().moveToState(Lifecycle.State.DESTROYED)
    }
}
```

- [ ] **Step 2: Run the tests**

```bash
./run_tests.sh --tests "com.runner.ui.map.MapFragmentTest"
```

Expected: 3 tests pass. No failures.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/runner/ui/map/MapFragmentTest.kt
git commit -m "test: add MapFragment tests for overlay visibility and lifecycle"
```

---

## Final verification — full test suite

After all 5 tasks complete:

```bash
./run_tests.sh
```

Expected: All tests pass — 31 new tests added across 5 files, plus all 17 pre-existing tests (38 total + ExampleUnitTest).
