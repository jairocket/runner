# Clear Map Trajectory on Save

**Date:** 2026-05-17
**Branch:** 3-clean-up-the-drawn-line-on-map-when-run-activity-is-saved
**Scope:** When the user taps Save in TrackingFragment, the drawn route on MapFragment is cleared.

---

## Problem

After a run is saved, `locationHistory` is not cleared and `MapFragment` has no signal to clear its polyline. The old trajectory persists on the map until a new tracking session starts.

## Approach

Add a `trajectorySaved` event LiveData to `LocationViewModel`. `resetTimer()` clears `_locationHistory` and posts to this LiveData. `MapFragment` observes the event and clears its polyline.

## Data Flow

```
buttonSave (TrackingFragment)
    → viewModel.resetTimer()
        → clears _locationHistory
        → resets timer / distance / pace  [existing]
        → _trajectorySaved.value = Unit   [new]

MapFragment observes trajectorySaved
    → routePolyline.setPoints(emptyList())
    → binding.mapView.invalidate()
```

## Changes

### `LocationViewModel`

- Add `private val _trajectorySaved = MutableLiveData<Unit>()`
- Expose `val trajectorySaved: LiveData<Unit> = _trajectorySaved`
- In `resetTimer()`: add `_locationHistory.clear()` and `_trajectorySaved.value = Unit`

### `MapFragment`

- In `onViewCreated`, observe `viewModel.trajectorySaved`
- On event: `routePolyline.setPoints(emptyList())` + `binding.mapView.invalidate()`

### No changes to `TrackingFragment`

`buttonSave` already calls `viewModel.resetTimer()`.

## Future Persistence Compatibility

When real persistence is added, `resetTimer()` will be replaced by a `saveRun()` method that reads current state and hands it to a repository before clearing. The trajectory clear and `_trajectorySaved` emission remain unchanged — persistence is inserted before them.

## Out of Scope

- Persisting the run to the history list
- Any UI confirmation of the save action
