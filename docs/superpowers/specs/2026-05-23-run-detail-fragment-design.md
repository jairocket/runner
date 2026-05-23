# Run Detail Fragment Design

**Date:** 2026-05-23
**Status:** Approved

## Overview

When a user taps a run in `HistoryFragment`, a `RunDetailFragment` opens showing that run's stats and its GPS route drawn on an embedded map. All data is mocked now; the repository layer is designed for backend migration.

---

## Data Model

### `Position`

A lightweight, `Parcelable` coordinate class. Kept framework-agnostic (not osmdroid's `GeoPoint`) so it can be serialized and stored independently of the map library.

Uses manual `Parcelable` (not `@Parcelize`) because the `kotlin-parcelize` compiler plugin is incompatible with AGP 9.0.0-alpha06's embedded Kotlin 2.2.10.

```kotlin
data class Position(val lat: Double, val lon: Double) : Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(lat)
        parcel.writeDouble(lon)
    }
    companion object CREATOR : Parcelable.Creator<Position> {
        override fun createFromParcel(parcel: Parcel) = Position(parcel.readDouble(), parcel.readDouble())
        override fun newArray(size: Int): Array<Position?> = arrayOfNulls(size)
    }
}
```

### `RunActivity`

Adds `id` (used for navigation lookup) and `positions` (the ordered GPS trace for the run).

```kotlin
data class RunActivity(
    val id: String,
    val date: String,
    val duration: String,
    val distanceKm: String,
    val paceMinKm: String,
    val positions: List<Position>
)
```

`HistoryFragment` only needs the metadata fields to render the list. `positions` is loaded on demand by `RunDetailFragment` via the repository.

---

## Repository Layer

### `RunRepository` interface

The contract that all ViewModels depend on. No ViewModel imports a concrete implementation.

```kotlin
interface RunRepository {
    fun getAll(): List<RunActivity>
    fun getById(id: String): RunActivity?
}
```

### `MockRunRepository`

Concrete implementation for the current mock-data phase.

```kotlin
class MockRunRepository : RunRepository {
    override fun getAll(): List<RunActivity> = mockRuns
    override fun getById(id: String): RunActivity? = mockRuns.find { it.id == id }
}
```

Mock data lives in a top-level `val mockRuns: List<RunActivity>` in the `history` package, with realistic coordinates for each run.

### Future implementations

`SupabaseRunRepository : RunRepository` and/or `CustomBackendRunRepository : RunRepository` can be added without touching any ViewModel or Fragment. When Dependency Injection (Hilt) is introduced, the factory pattern is removed and the interface is injected directly.

`UserRepository` is deferred to its own spec (auth/authorization concern).

---

## ViewModels

### `HistoryViewModel`

Loads the full run list for `HistoryFragment`.

```kotlin
class HistoryViewModel(private val repo: RunRepository) : ViewModel() {
    val runs: List<RunActivity> = repo.getAll()
}
```

### `RunDetailViewModel`

Loads a single run by ID for `RunDetailFragment`. Receives `repo` and `runId` via a `Factory`.

```kotlin
class RunDetailViewModel(
    private val repo: RunRepository,
    private val runId: String
) : ViewModel() {
    val run: RunActivity? = repo.getById(runId)
}
```

Both ViewModels use a `ViewModelProvider.Factory` that instantiates `MockRunRepository()`. This is the only place that references the concrete class — the single seam to swap when DI arrives.

---

## Navigation

`RunDetailFragment` is a sub-destination of `HistoryFragment` in the nav graph. It is **not** a bottom-nav top-level destination. Back-pressing from detail returns to the history list.

Nav graph additions — the `<action>` is nested inside the `HistoryFragment` element, and `RunDetailFragment` is a plain destination with no `<argument>` declaration (no Safe Args):

```xml
<fragment
    android:id="@+id/HistoryFragment"
    android:name="com.runner.ui.history.HistoryFragment"
    ...>
    <action
        android:id="@+id/action_history_to_detail"
        app:destination="@id/RunDetailFragment"/>
</fragment>

<fragment
    android:id="@+id/RunDetailFragment"
    android:name="com.runner.ui.history.RunDetailFragment"
    android:label="Run Detail"
    tools:layout="@layout/fragment_run_detail"/>
```

Navigation is triggered from `HistoryFragment` using a plain `Bundle` (Safe Args not used — a single string ID doesn't justify the extra plugin dependency):

```kotlin
val bundle = Bundle().apply { putString("runId", run.id) }
findNavController().navigate(R.id.action_history_to_detail, bundle)
```

---

## HistoryAdapter Changes

Gains an `onItemClick` callback. The adapter itself does not import navigation — it only reports clicks back to the fragment.

```kotlin
class HistoryAdapter(
    private val items: List<RunActivity>,
    private val onItemClick: (RunActivity) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>()
```

---

## RunDetailFragment Layout (`fragment_run_detail.xml`)

Two sections stacked vertically in a `LinearLayout`:

1. **Stats header** — date, distance, duration, pace. Same visual style and color tokens as `item_history_run.xml`.
2. **Embedded `MapView`** — fills the remaining space (`layout_weight="1"`). Read-only; no live tracking, no loading overlay.

```
┌─────────────────────────────┐
│  May 14, 2026               │
│  6.2 km      42:17  6:49/km │
├─────────────────────────────┤
│                             │
│        osmdroid MapView     │
│        (polyline route)     │
│                             │
└─────────────────────────────┘
```

---

## RunDetailFragment Logic

1. Read `runId` from `arguments` using `RunDetailFragment.ARG_RUN_ID = "runId"` constant.
2. Instantiate `RunDetailViewModel` via factory.
3. Bind `run.date`, `run.distanceKm`, `run.duration`, `run.paceMinKm` to stats views.
4. Convert `run.positions: List<Position>` → `List<GeoPoint>`.
5. Set points on a `Polyline` overlay and add it to the `MapView`.
6. Center the map on the midpoint of the route (`positions[positions.size / 2]`).

osmdroid setup mirrors `MapFragment`: `Configuration.getInstance().load(...)`, `setTileSource(MAPNIK)`, `setMultiTouchControls(true)`, zoom 17 (suited for short neighbourhood routes).

`MapView.onResume()` / `onPause()` are forwarded from the fragment lifecycle, same as `MapFragment`.

---

## File Checklist

| File | Change |
|------|--------|
| `ui/history/Position.kt` | New — coordinate data class |
| `ui/history/RunActivity.kt` | Add `id`, `positions` fields |
| `ui/history/RunRepository.kt` | New — interface |
| `ui/history/MockRunRepository.kt` | New — mock implementation with position data |
| `ui/history/HistoryViewModel.kt` | New — replaces inline mock list in fragment |
| `ui/history/HistoryFragment.kt` | Use `HistoryViewModel`; wire click → navigate |
| `ui/history/HistoryAdapter.kt` | Add `onItemClick` callback |
| `ui/history/RunDetailViewModel.kt` | New |
| `ui/history/RunDetailFragment.kt` | New |
| `res/layout/fragment_run_detail.xml` | New |
| `res/navigation/nav_graph.xml` | Add `RunDetailFragment` destination + action |

---

## Out of Scope

- Persistence (Room, Supabase, custom backend)
- `UserRepository` / authentication — deferred to separate spec
- Dependency Injection (Hilt)
- Edit or delete a run from the detail screen
