# Run Detail Fragment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tap a run in HistoryFragment to open RunDetailFragment, which shows run stats and draws the GPS route on an embedded osmdroid map.

**Architecture:** `HistoryFragment` navigates to `RunDetailFragment` passing only a `runId` string via Bundle. Each fragment has its own ViewModel backed by a `RunRepository` interface, with `MockRunRepository` as the current implementation — the seam for future Supabase/custom backend swap. No Safe Args — Bundle with a `ARG_RUN_ID` constant.

**Tech Stack:** Kotlin, AndroidX Navigation Component, osmdroid 6.1.18, Robolectric 4.11.1 (tests via `./run_tests.sh`), View Binding. Note: `kotlin-parcelize` plugin is incompatible with AGP 9.0.0-alpha06's embedded Kotlin — `LatLng` uses manual `Parcelable` instead of `@Parcelize`.

---

## File Map

| File | Action |
|------|--------|
| `app/build.gradle.kts` | Add `kotlin-parcelize` plugin |
| `ui/history/LatLng.kt` | **New** — `@Parcelize` coordinate data class |
| `ui/history/RunActivity.kt` | Add `id: String`, `positions: List<LatLng>` |
| `ui/history/MockRuns.kt` | **New** — top-level `mockRuns` val with 6 runs + coordinates |
| `ui/history/RunRepository.kt` | **New** — interface with `getAll()` / `getById()` |
| `ui/history/MockRunRepository.kt` | **New** — implements `RunRepository` using `mockRuns` |
| `ui/history/HistoryViewModel.kt` | **New** — wraps `repo.getAll()`, factory in companion object |
| `ui/history/HistoryAdapter.kt` | Add `onItemClick: (RunActivity) -> Unit` parameter |
| `ui/history/HistoryFragment.kt` | Use `HistoryViewModel`; adapter click → `navigate()` |
| `res/navigation/nav_graph.xml` | Add `RunDetailFragment` destination + action on `HistoryFragment` |
| `ui/history/RunDetailViewModel.kt` | **New** — wraps `repo.getById(runId)`, factory in companion object |
| `res/layout/fragment_run_detail.xml` | **New** — stats header + embedded `MapView` |
| `ui/history/RunDetailFragment.kt` | **New** — reads `runId` from args, binds stats, draws polyline |
| `test/ui/history/LatLngTest.kt` | **New** |
| `test/ui/history/MockRunRepositoryTest.kt` | **New** |
| `test/ui/history/HistoryViewModelTest.kt` | **New** |
| `test/ui/history/HistoryAdapterTest.kt` | Update — fix `RunActivity` constructor + add callback arg + click test |
| `test/ui/history/HistoryFragmentTest.kt` | Update — no constructor changes needed (uses ViewModel) |
| `test/ui/history/RunDetailViewModelTest.kt` | **New** |
| `test/ui/history/RunDetailFragmentTest.kt` | **New** |

---

## ✅ Task 1: Add `kotlin-parcelize` Plugin (DONE — plugin abandoned, build.gradle.kts clean)

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add the plugin**

Open `app/build.gradle.kts` and update the `plugins` block:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("kotlin-parcelize")
}
```

- [ ] **Step 2: Verify the build compiles**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build: add kotlin-parcelize plugin"
```

---

## ✅ Task 2: Create `LatLng` Data Class (DONE — manual Parcelable, not @Parcelize)

**Files:**
- Create: `app/src/test/java/com/runner/ui/history/LatLngTest.kt`
- Create: `app/src/main/java/com/runner/ui/history/LatLng.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/runner/ui/history/LatLngTest.kt`:

```kotlin
package com.runner.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class LatLngTest {

    @Test
    fun constructor_storesLatAndLon() {
        val point = LatLng(-25.4284, -49.2733)
        assertEquals(-25.4284, point.lat, 0.0001)
        assertEquals(-49.2733, point.lon, 0.0001)
    }

    @Test
    fun dataClass_equalityByValue() {
        val a = LatLng(-25.4284, -49.2733)
        val b = LatLng(-25.4284, -49.2733)
        assertEquals(a, b)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./run_tests.sh --tests "com.runner.ui.history.LatLngTest"
```

Expected: compilation error — `LatLng` not found.

- [ ] **Step 3: Create `LatLng.kt`**

Create `app/src/main/java/com/runner/ui/history/LatLng.kt`:

```kotlin
package com.runner.ui.history

import android.os.Parcel
import android.os.Parcelable

data class LatLng(val lat: Double, val lon: Double) : Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(lat)
        parcel.writeDouble(lon)
    }

    companion object CREATOR : Parcelable.Creator<LatLng> {
        override fun createFromParcel(parcel: Parcel) = LatLng(parcel.readDouble(), parcel.readDouble())
        override fun newArray(size: Int): Array<LatLng?> = arrayOfNulls(size)
    }
}
```

- [ ] **Step 4: Run to confirm passing**

```bash
./run_tests.sh --tests "com.runner.ui.history.LatLngTest"
```

Expected: `BUILD SUCCESSFUL`, 2 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/runner/ui/history/LatLng.kt \
        app/src/test/java/com/runner/ui/history/LatLngTest.kt
git commit -m "feat: add LatLng parcelable coordinate class"
```

---

## ✅ Task 3: Update `RunActivity` + Create `MockRuns.kt` + Fix Compilation (DONE)

`RunActivity` gains `id` and `positions`. Anything constructing `RunActivity` with the old 4-arg signature must be updated immediately.

**Files:**
- Modify: `app/src/main/java/com/runner/ui/history/RunActivity.kt`
- Create: `app/src/main/java/com/runner/ui/history/MockRuns.kt`
- Modify: `app/src/test/java/com/runner/ui/history/HistoryAdapterTest.kt`
- Modify: `app/src/main/java/com/runner/ui/history/HistoryFragment.kt` (temporary fix only)

- [ ] **Step 1: Update `RunActivity.kt`**

Replace the full file contents:

```kotlin
package com.runner.ui.history

data class RunActivity(
    val id: String,
    val date: String,
    val duration: String,
    val distanceKm: String,
    val paceMinKm: String,
    val positions: List<LatLng>
)
```

- [ ] **Step 2: Create `MockRuns.kt`**

Create `app/src/main/java/com/runner/ui/history/MockRuns.kt`:

```kotlin
package com.runner.ui.history

val mockRuns = listOf(
    RunActivity(
        id = "1",
        date = "May 14, 2026",
        duration = "42:17",
        distanceKm = "6.2 km",
        paceMinKm = "6:49",
        positions = listOf(
            LatLng(-25.4284, -49.2733),
            LatLng(-25.4270, -49.2750),
            LatLng(-25.4255, -49.2768),
            LatLng(-25.4240, -49.2755),
            LatLng(-25.4228, -49.2735),
            LatLng(-25.4235, -49.2710),
            LatLng(-25.4250, -49.2695),
            LatLng(-25.4265, -49.2705),
            LatLng(-25.4278, -49.2720),
            LatLng(-25.4284, -49.2733)
        )
    ),
    RunActivity(
        id = "2",
        date = "May 12, 2026",
        duration = "31:04",
        distanceKm = "4.8 km",
        paceMinKm = "6:28",
        positions = listOf(
            LatLng(-25.4350, -49.2650),
            LatLng(-25.4335, -49.2668),
            LatLng(-25.4318, -49.2682),
            LatLng(-25.4305, -49.2660),
            LatLng(-25.4315, -49.2638),
            LatLng(-25.4332, -49.2625),
            LatLng(-25.4348, -49.2635),
            LatLng(-25.4350, -49.2650)
        )
    ),
    RunActivity(
        id = "3",
        date = "May 10, 2026",
        duration = "58:33",
        distanceKm = "9.1 km",
        paceMinKm = "6:26",
        positions = listOf(
            LatLng(-25.4420, -49.2800),
            LatLng(-25.4400, -49.2825),
            LatLng(-25.4378, -49.2848),
            LatLng(-25.4355, -49.2830),
            LatLng(-25.4340, -49.2808),
            LatLng(-25.4352, -49.2782),
            LatLng(-25.4370, -49.2765),
            LatLng(-25.4392, -49.2778),
            LatLng(-25.4408, -49.2792),
            LatLng(-25.4420, -49.2800)
        )
    ),
    RunActivity(
        id = "4",
        date = "May 7, 2026",
        duration = "22:45",
        distanceKm = "3.5 km",
        paceMinKm = "6:30",
        positions = listOf(
            LatLng(-25.4180, -49.2600),
            LatLng(-25.4165, -49.2615),
            LatLng(-25.4152, -49.2600),
            LatLng(-25.4160, -49.2582),
            LatLng(-25.4175, -49.2575),
            LatLng(-25.4185, -49.2588),
            LatLng(-25.4180, -49.2600)
        )
    ),
    RunActivity(
        id = "5",
        date = "May 5, 2026",
        duration = "45:12",
        distanceKm = "7.0 km",
        paceMinKm = "6:27",
        positions = listOf(
            LatLng(-25.4500, -49.2700),
            LatLng(-25.4480, -49.2722),
            LatLng(-25.4458, -49.2740),
            LatLng(-25.4440, -49.2720),
            LatLng(-25.4428, -49.2698),
            LatLng(-25.4440, -49.2675),
            LatLng(-25.4460, -49.2660),
            LatLng(-25.4480, -49.2675),
            LatLng(-25.4498, -49.2688),
            LatLng(-25.4500, -49.2700)
        )
    ),
    RunActivity(
        id = "6",
        date = "May 3, 2026",
        duration = "35:50",
        distanceKm = "5.5 km",
        paceMinKm = "6:31",
        positions = listOf(
            LatLng(-25.4310, -49.2850),
            LatLng(-25.4292, -49.2870),
            LatLng(-25.4275, -49.2858),
            LatLng(-25.4268, -49.2835),
            LatLng(-25.4280, -49.2815),
            LatLng(-25.4298, -49.2808),
            LatLng(-25.4312, -49.2825),
            LatLng(-25.4310, -49.2850)
        )
    )
)
```

- [ ] **Step 3: Fix `HistoryAdapterTest.kt` — update `RunActivity` constructor calls**

In `HistoryAdapterTest`, `sampleItems` uses the old 4-arg `RunActivity` constructor and `HistoryAdapter` is constructed without a callback. Update both. Replace the `sampleItems` val and the `makeHolder()` function:

```kotlin
private val sampleItems = listOf(
    RunActivity("1", "May 14, 2026", "42:17", "6.2 km", "6:49", emptyList()),
    RunActivity("2", "May 12, 2026", "31:04", "4.8 km", "6:28", emptyList())
)

private fun makeHolder(): HistoryAdapter.ViewHolder =
    HistoryAdapter(sampleItems) {}.onCreateViewHolder(FrameLayout(context), 0)
```

Also update every `HistoryAdapter(sampleItems)` call in the test body to `HistoryAdapter(sampleItems) {}`:

```kotlin
@Test
fun getItemCount_returnsListSize() {
    assertEquals(2, HistoryAdapter(sampleItems) {}.itemCount)
}

@Test
fun getItemCount_emptyList_returnsZero() {
    assertEquals(0, HistoryAdapter(emptyList()) {}.itemCount)
}

@Test
fun bind_displaysDate() {
    val adapter = HistoryAdapter(sampleItems) {}
    val holder = makeHolder()
    adapter.onBindViewHolder(holder, 0)
    assertEquals("May 14, 2026", holder.itemView.findViewById<TextView>(R.id.textItemDate).text.toString())
}

@Test
fun bind_displaysDistance() {
    val adapter = HistoryAdapter(sampleItems) {}
    val holder = makeHolder()
    adapter.onBindViewHolder(holder, 0)
    assertEquals("6.2 km", holder.itemView.findViewById<TextView>(R.id.textItemDistance).text.toString())
}

@Test
fun bind_displaysDuration() {
    val adapter = HistoryAdapter(sampleItems) {}
    val holder = makeHolder()
    adapter.onBindViewHolder(holder, 0)
    assertEquals("42:17", holder.itemView.findViewById<TextView>(R.id.textItemDuration).text.toString())
}

@Test
fun bind_displaysPaceWithMinKmSuffix() {
    val adapter = HistoryAdapter(sampleItems) {}
    val holder = makeHolder()
    adapter.onBindViewHolder(holder, 0)
    assertEquals("6:49 min/km", holder.itemView.findViewById<TextView>(R.id.textItemPace).text.toString())
}

@Test
fun bind_secondItem_displaysCorrectDate() {
    val adapter = HistoryAdapter(sampleItems) {}
    val holder = makeHolder()
    adapter.onBindViewHolder(holder, 1)
    assertEquals("May 12, 2026", holder.itemView.findViewById<TextView>(R.id.textItemDate).text.toString())
}
```

- [ ] **Step 4: Temporarily fix `HistoryFragment.kt` inline mock**

`HistoryFragment` still has the old inline `mockRuns` list. Update only the `RunActivity` constructors to use 6 args so it compiles. (Task 7 will replace this entire block with a ViewModel call.)

Find the `mockRuns` list in `HistoryFragment.onViewCreated` and replace it:

```kotlin
val mockRuns = listOf(
    RunActivity("1", "May 14, 2026", "42:17", "6.2 km", "6:49", emptyList()),
    RunActivity("2", "May 12, 2026", "31:04", "4.8 km", "6:28", emptyList()),
    RunActivity("3", "May 10, 2026", "58:33", "9.1 km", "6:26", emptyList()),
    RunActivity("4", "May 7, 2026",  "22:45", "3.5 km", "6:30", emptyList()),
    RunActivity("5", "May 5, 2026",  "45:12", "7.0 km", "6:27", emptyList()),
    RunActivity("6", "May 3, 2026",  "35:50", "5.5 km", "6:31", emptyList())
)
```

Also update `HistoryAdapter(mockRuns)` → `HistoryAdapter(mockRuns) {}` in `HistoryFragment`.

> Note: `RunDetailFragment` does not exist yet — do not reference it by class name here. The string `"runId"` is used directly in Task 7 and matched by `ARG_RUN_ID = "runId"` in Task 10.

- [ ] **Step 5: Run all history tests**

```bash
./run_tests.sh --tests "com.runner.ui.history"
```

Expected: all tests PASS (HistoryAdapterTest, HistoryFragmentTest, LatLngTest).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/runner/ui/history/RunActivity.kt \
        app/src/main/java/com/runner/ui/history/MockRuns.kt \
        app/src/main/java/com/runner/ui/history/HistoryFragment.kt \
        app/src/test/java/com/runner/ui/history/HistoryAdapterTest.kt
git commit -m "feat: add id and positions to RunActivity, add mock run data"
```

---

## ✅ Task 4: `RunRepository` Interface + `MockRunRepository` (DONE)

**Files:**
- Create: `app/src/test/java/com/runner/ui/history/MockRunRepositoryTest.kt`
- Create: `app/src/main/java/com/runner/ui/history/RunRepository.kt`
- Create: `app/src/main/java/com/runner/ui/history/MockRunRepository.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/runner/ui/history/MockRunRepositoryTest.kt`:

```kotlin
package com.runner.ui.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MockRunRepositoryTest {

    private val repo = MockRunRepository()

    @Test
    fun getAll_returnsSixRuns() {
        assertEquals(6, repo.getAll().size)
    }

    @Test
    fun getById_returnsCorrectRun() {
        val run = repo.getById("1")
        assertEquals("May 14, 2026", run?.date)
        assertEquals("6.2 km", run?.distanceKm)
    }

    @Test
    fun getById_unknownId_returnsNull() {
        assertNull(repo.getById("999"))
    }

    @Test
    fun getAll_firstRunHasPositions() {
        val run = repo.getAll().first()
        assert(run.positions.isNotEmpty())
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./run_tests.sh --tests "com.runner.ui.history.MockRunRepositoryTest"
```

Expected: compilation error — `MockRunRepository` not found.

- [ ] **Step 3: Create `RunRepository.kt`**

Create `app/src/main/java/com/runner/ui/history/RunRepository.kt`:

```kotlin
package com.runner.ui.history

interface RunRepository {
    fun getAll(): List<RunActivity>
    fun getById(id: String): RunActivity?
}
```

- [ ] **Step 4: Create `MockRunRepository.kt`**

Create `app/src/main/java/com/runner/ui/history/MockRunRepository.kt`:

```kotlin
package com.runner.ui.history

class MockRunRepository : RunRepository {
    override fun getAll(): List<RunActivity> = mockRuns
    override fun getById(id: String): RunActivity? = mockRuns.find { it.id == id }
}
```

- [ ] **Step 5: Run to confirm passing**

```bash
./run_tests.sh --tests "com.runner.ui.history.MockRunRepositoryTest"
```

Expected: `BUILD SUCCESSFUL`, 4 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/runner/ui/history/RunRepository.kt \
        app/src/main/java/com/runner/ui/history/MockRunRepository.kt \
        app/src/test/java/com/runner/ui/history/MockRunRepositoryTest.kt
git commit -m "feat: add RunRepository interface and MockRunRepository"
```

---

## ✅ Task 5: `HistoryViewModel` (DONE)

**Files:**
- Create: `app/src/test/java/com/runner/ui/history/HistoryViewModelTest.kt`
- Create: `app/src/main/java/com/runner/ui/history/HistoryViewModel.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/runner/ui/history/HistoryViewModelTest.kt`:

```kotlin
package com.runner.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryViewModelTest {

    private fun makeRepo(runs: List<RunActivity> = mockRuns): RunRepository =
        object : RunRepository {
            override fun getAll() = runs
            override fun getById(id: String) = runs.find { it.id == id }
        }

    @Test
    fun runs_returnsAllRunsFromRepo() {
        val vm = HistoryViewModel(makeRepo())
        assertEquals(6, vm.runs.size)
    }

    @Test
    fun runs_returnsCorrectFirstRun() {
        val vm = HistoryViewModel(makeRepo())
        assertEquals("May 14, 2026", vm.runs.first().date)
    }

    @Test
    fun runs_emptyRepo_returnsEmptyList() {
        val vm = HistoryViewModel(makeRepo(emptyList()))
        assertEquals(0, vm.runs.size)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./run_tests.sh --tests "com.runner.ui.history.HistoryViewModelTest"
```

Expected: compilation error — `HistoryViewModel` not found.

- [ ] **Step 3: Create `HistoryViewModel.kt`**

Create `app/src/main/java/com/runner/ui/history/HistoryViewModel.kt`:

```kotlin
package com.runner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class HistoryViewModel(private val repo: RunRepository) : ViewModel() {

    val runs: List<RunActivity> = repo.getAll()

    companion object {
        fun factory(repo: RunRepository = MockRunRepository()): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(repo) as T
            }
    }
}
```

- [ ] **Step 4: Run to confirm passing**

```bash
./run_tests.sh --tests "com.runner.ui.history.HistoryViewModelTest"
```

Expected: `BUILD SUCCESSFUL`, 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/runner/ui/history/HistoryViewModel.kt \
        app/src/test/java/com/runner/ui/history/HistoryViewModelTest.kt
git commit -m "feat: add HistoryViewModel backed by RunRepository"
```

---

## ✅ Task 6: Update `HistoryAdapter` with Click Callback (DONE)

**Files:**
- Modify: `app/src/main/java/com/runner/ui/history/HistoryAdapter.kt`
- Modify: `app/src/test/java/com/runner/ui/history/HistoryAdapterTest.kt`

- [ ] **Step 1: Write the new failing test**

Add this test to the bottom of `HistoryAdapterTest`:

```kotlin
@Test
fun clickingItem_invokesCallback_withCorrectRun() {
    var clicked: RunActivity? = null
    val adapter = HistoryAdapter(sampleItems) { run -> clicked = run }
    val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
    adapter.onBindViewHolder(holder, 0)
    holder.itemView.performClick()
    assertEquals(sampleItems[0], clicked)
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./run_tests.sh --tests "com.runner.ui.history.HistoryAdapterTest"
```

Expected: `clickingItem_invokesCallback_withCorrectRun` FAILS — click does nothing yet.

- [ ] **Step 3: Update `HistoryAdapter.kt`**

Replace the full file:

```kotlin
package com.runner.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.runner.databinding.ItemHistoryRunBinding

class HistoryAdapter(
    private val items: List<RunActivity>,
    private val onItemClick: (RunActivity) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemHistoryRunBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RunActivity) {
            binding.textItemDate.text = item.date
            binding.textItemDistance.text = item.distanceKm
            binding.textItemDuration.text = item.duration
            binding.textItemPace.text = "${item.paceMinKm} min/km"
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryRunBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
```

- [ ] **Step 4: Run to confirm all adapter tests pass**

```bash
./run_tests.sh --tests "com.runner.ui.history.HistoryAdapterTest"
```

Expected: `BUILD SUCCESSFUL`, all tests PASS including the new click test.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/runner/ui/history/HistoryAdapter.kt \
        app/src/test/java/com/runner/ui/history/HistoryAdapterTest.kt
git commit -m "feat: add click callback to HistoryAdapter"
```

---

## ✅ Task 7: Update `HistoryFragment` + Nav Graph (DONE — ⚠️ tests failing, diagnose before resuming)

Replace the inline mock list with `HistoryViewModel` and wire the click to navigate. Also add `RunDetailFragment` to the nav graph so navigation compiles.

**Files:**
- Modify: `app/src/main/java/com/runner/ui/history/HistoryFragment.kt`
- Modify: `app/src/main/res/navigation/nav_graph.xml`

- [ ] **Step 1: Update `nav_graph.xml`**

Open `app/src/main/res/navigation/nav_graph.xml`. Add a `RunDetailFragment` destination and an action inside the `HistoryFragment` element:

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:app="http://schemas.android.com/apk/res-auto"
            xmlns:tools="http://schemas.android.com/tools"
            android:id="@+id/nav_graph"
            app:startDestination="@id/TrackingFragment">

    <fragment
        android:id="@+id/TrackingFragment"
        android:name="com.runner.ui.tracking.TrackingFragment"
        android:label="@string/first_fragment_label"
        tools:layout="@layout/fragment_tracking"/>

    <fragment
        android:id="@+id/HistoryFragment"
        android:name="com.runner.ui.history.HistoryFragment"
        android:label="@string/second_fragment_label"
        tools:layout="@layout/fragment_second">
        <action
            android:id="@+id/action_history_to_detail"
            app:destination="@id/RunDetailFragment"/>
    </fragment>

    <fragment
        android:id="@+id/MapFragment"
        android:name="com.runner.ui.map.MapFragment"
        android:label="Map"
        tools:layout="@layout/fragment_map"/>

    <fragment
        android:id="@+id/RunDetailFragment"
        android:name="com.runner.ui.history.RunDetailFragment"
        android:label="Run Detail"
        tools:layout="@layout/fragment_run_detail"/>

</navigation>
```

- [ ] **Step 2: Update `HistoryFragment.kt`**

Replace the full file:

```kotlin
package com.runner.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.runner.R
import com.runner.databinding.FragmentSecondBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels { HistoryViewModel.factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HistoryAdapter(viewModel.runs) { run ->
                val bundle = Bundle().apply { putString("runId", run.id) }
                findNavController().navigate(R.id.action_history_to_detail, bundle)
            }
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

- [ ] **Step 3: Run all history tests**

```bash
./run_tests.sh --tests "com.runner.ui.history"
```

Expected: all tests PASS. `recyclerView_adapter_has6Items` still passes because `HistoryViewModel.factory()` uses `MockRunRepository` which returns 6 items.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/runner/ui/history/HistoryFragment.kt \
        app/src/main/res/navigation/nav_graph.xml
git commit -m "feat: wire HistoryFragment to HistoryViewModel and navigate to RunDetailFragment"
```

---

## Task 8: `RunDetailViewModel`

**Files:**
- Create: `app/src/test/java/com/runner/ui/history/RunDetailViewModelTest.kt`
- Create: `app/src/main/java/com/runner/ui/history/RunDetailViewModel.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/runner/ui/history/RunDetailViewModelTest.kt`:

```kotlin
package com.runner.ui.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunDetailViewModelTest {

    private fun makeRepo(runs: List<RunActivity> = mockRuns): RunRepository =
        object : RunRepository {
            override fun getAll() = runs
            override fun getById(id: String) = runs.find { it.id == id }
        }

    @Test
    fun run_returnsCorrectRunForValidId() {
        val vm = RunDetailViewModel(makeRepo(), "1")
        assertEquals("May 14, 2026", vm.run?.date)
        assertEquals("6.2 km", vm.run?.distanceKm)
    }

    @Test
    fun run_returnsNullForUnknownId() {
        val vm = RunDetailViewModel(makeRepo(), "999")
        assertNull(vm.run)
    }

    @Test
    fun run_positionsArePresent() {
        val vm = RunDetailViewModel(makeRepo(), "1")
        assert((vm.run?.positions?.size ?: 0) > 0)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./run_tests.sh --tests "com.runner.ui.history.RunDetailViewModelTest"
```

Expected: compilation error — `RunDetailViewModel` not found.

- [ ] **Step 3: Create `RunDetailViewModel.kt`**

Create `app/src/main/java/com/runner/ui/history/RunDetailViewModel.kt`:

```kotlin
package com.runner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RunDetailViewModel(
    private val repo: RunRepository,
    private val runId: String
) : ViewModel() {

    val run: RunActivity? = repo.getById(runId)

    companion object {
        fun factory(runId: String, repo: RunRepository = MockRunRepository()): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RunDetailViewModel(repo, runId) as T
            }
    }
}
```

- [ ] **Step 4: Run to confirm passing**

```bash
./run_tests.sh --tests "com.runner.ui.history.RunDetailViewModelTest"
```

Expected: `BUILD SUCCESSFUL`, 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/runner/ui/history/RunDetailViewModel.kt \
        app/src/test/java/com/runner/ui/history/RunDetailViewModelTest.kt
git commit -m "feat: add RunDetailViewModel backed by RunRepository"
```

---

## Task 9: `fragment_run_detail.xml` Layout

**Files:**
- Create: `app/src/main/res/layout/fragment_run_detail.xml`

- [ ] **Step 1: Create the layout**

Create `app/src/main/res/layout/fragment_run_detail.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/colorBackground">

    <!-- Stats header -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingStart="24dp"
        android:paddingEnd="24dp"
        android:paddingTop="20dp"
        android:paddingBottom="20dp">

        <!-- Left: date + distance -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/textDetailDate"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/colorTextMuted"
                android:textSize="10sp"
                android:letterSpacing="0.2"
                android:fontFamily="monospace"
                android:layout_marginBottom="6dp"
                tools:text="May 14, 2026"/>

            <TextView
                android:id="@+id/textDetailDistance"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/colorAccentLime"
                android:textSize="28sp"
                android:fontFamily="monospace"
                android:includeFontPadding="false"
                tools:text="6.2 km"/>

        </LinearLayout>

        <!-- Right: duration + pace -->
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:gravity="end">

            <TextView
                android:id="@+id/textDetailDuration"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/colorTextPrimary"
                android:textSize="18sp"
                android:fontFamily="monospace"
                android:includeFontPadding="false"
                tools:text="42:17"/>

            <TextView
                android:id="@+id/textDetailPace"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/colorTextMuted"
                android:textSize="11sp"
                android:fontFamily="monospace"
                android:layout_marginTop="4dp"
                tools:text="6:49 min/km"/>

        </LinearLayout>

    </LinearLayout>

    <!-- Map fills the rest -->
    <org.osmdroid.views.MapView
        android:id="@+id/mapViewDetail"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

</LinearLayout>
```

- [ ] **Step 2: Verify build**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/fragment_run_detail.xml
git commit -m "feat: add fragment_run_detail layout with stats header and map"
```

---

## Task 10: `RunDetailFragment`

**Files:**
- Create: `app/src/test/java/com/runner/ui/history/RunDetailFragmentTest.kt`
- Create: `app/src/main/java/com/runner/ui/history/RunDetailFragment.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/runner/ui/history/RunDetailFragmentTest.kt`:

```kotlin
package com.runner.ui.history

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runner.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class RunDetailFragmentTest {

    private fun argsFor(runId: String) = Bundle().apply {
        putString(RunDetailFragment.ARG_RUN_ID, runId)
    }

    @Test
    fun statsAreDisplayed_forValidRun() {
        val run = MockRunRepository().getById("1")!!
        launchFragmentInContainer<RunDetailFragment>(
            fragmentArgs = argsFor("1"),
            themeResId = R.style.Theme_Runner
        ).onFragment { fragment ->
            val view = fragment.requireView()
            assertEquals(run.date, view.findViewById<TextView>(R.id.textDetailDate).text.toString())
            assertEquals(run.distanceKm, view.findViewById<TextView>(R.id.textDetailDistance).text.toString())
            assertEquals(run.duration, view.findViewById<TextView>(R.id.textDetailDuration).text.toString())
            assertEquals("${run.paceMinKm} min/km", view.findViewById<TextView>(R.id.textDetailPace).text.toString())
        }
    }

    @Test
    fun fragment_launchesWithoutCrash_forUnknownRunId() {
        launchFragmentInContainer<RunDetailFragment>(
            fragmentArgs = argsFor("999"),
            themeResId = R.style.Theme_Runner
        ).moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun binding_onDestroyView_doesNotLeak() {
        launchFragmentInContainer<RunDetailFragment>(
            fragmentArgs = argsFor("1"),
            themeResId = R.style.Theme_Runner
        ).moveToState(Lifecycle.State.DESTROYED)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./run_tests.sh --tests "com.runner.ui.history.RunDetailFragmentTest"
```

Expected: compilation error — `RunDetailFragment` not found.

- [ ] **Step 3: Create `RunDetailFragment.kt`**

Create `app/src/main/java/com/runner/ui/history/RunDetailFragment.kt`:

```kotlin
package com.runner.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.runner.databinding.FragmentRunDetailBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline

class RunDetailFragment : Fragment() {

    private var _binding: FragmentRunDetailBinding? = null
    private val binding get() = _binding!!

    private val runId: String by lazy { requireArguments().getString(ARG_RUN_ID)!! }
    private val viewModel: RunDetailViewModel by viewModels { RunDetailViewModel.factory(runId) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(
            requireContext(),
            android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName
        _binding = FragmentRunDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val run = viewModel.run ?: return

        binding.textDetailDate.text = run.date
        binding.textDetailDistance.text = run.distanceKm
        binding.textDetailDuration.text = run.duration
        binding.textDetailPace.text = "${run.paceMinKm} min/km"

        val points = run.positions.map { GeoPoint(it.lat, it.lon) }
        val polyline = Polyline().apply { setPoints(points) }

        binding.mapViewDetail.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            overlays.add(polyline)
            if (points.isNotEmpty()) {
                controller.setCenter(points[points.size / 2])
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapViewDetail.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapViewDetail.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_RUN_ID = "runId"
    }
}
```

- [ ] **Step 4: Run to confirm passing**

```bash
./run_tests.sh --tests "com.runner.ui.history.RunDetailFragmentTest"
```

Expected: `BUILD SUCCESSFUL`, 3 tests PASS.

- [ ] **Step 5: Run the full test suite**

```bash
./run_tests.sh
```

Expected: `BUILD SUCCESSFUL`, all tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/runner/ui/history/RunDetailFragment.kt \
        app/src/test/java/com/runner/ui/history/RunDetailFragmentTest.kt
git commit -m "feat: add RunDetailFragment with stats and route map"
```
