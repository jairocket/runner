# Map Initial Centering — Design Spec

**Date:** 2026-05-16
**Status:** Approved

## Problem

When the user opens the MapFragment, the map defaults to coordinates (0, 0) — a point in the Gulf of Guinea near western Africa — because no initial center is set. The map only repositions when a new GPS fix arrives *while tracking is active*, leaving users who open the map before starting a session looking at the wrong location.

## Goal

Center the map on the device's current location as soon as the MapFragment opens. If a GPS fix is not yet available, show a loading state and center the moment the first fix arrives.

## Design

### Components Changed

| File | Change |
|------|--------|
| `app/src/main/res/layout/fragment_map.xml` | Add loading overlay (spinner + label) |
| `app/src/main/java/com/runner/ui/map/MapFragment.kt` | Add initial centering logic + show/hide overlay |

### Layout

Add a full-screen `FrameLayout` overlay stacked above the `MapView` inside the existing `FrameLayout` root. The overlay contains:
- `CircularProgressIndicator` (centered)
- `TextView` with label "Getting your location…" (below the spinner)

The overlay is `VISIBLE` by default. The `MapView` renders behind it, so the user never sees the (0, 0) default position.

### MapFragment Logic

1. Add `private var hasInitialCenter = false` flag.
2. At the end of `onViewCreated`, after `drawHistory()`:
   - If `locationHistory` is not empty (prior session exists), hide the overlay immediately — the map is already centered on a real location by `drawHistory()`. Set `hasInitialCenter = true`.
   - Else if `viewModel.locationLiveData.value != null` (GPS fix available, no history), center on it, hide the overlay, set `hasInitialCenter = true`.
   - Else: leave the overlay visible and wait for the observer.
3. Extend the existing `locationLiveData` observer:
   - If `!hasInitialCenter`: center the map, hide the overlay, set `hasInitialCenter = true`.
   - Existing tracking path (polyline + animateTo) is unchanged, still gated on `isTracking == true`.

### Data Source

`LocationViewModel.locationLiveData` — already updated by `MainActivity`'s `FusedLocationProviderClient` on every GPS fix, independent of tracking state. No new location fetching needed.

### No Changes To

- `LocationViewModel` — no new fields or methods
- `MainActivity` — no changes
- Navigation graph — no new destinations
- Any other Fragment

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| GPS fix available when map opens | Center immediately, no spinner shown |
| No GPS fix yet | Spinner visible; center + dismiss on first fix |
| User opens map, fix arrives, user starts tracking | Centering already done; tracking observer adds polyline normally |
| `locationHistory` has points (prior session) | `drawHistory()` centers on last history point; overlay is hidden at the same time |
