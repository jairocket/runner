# Bottom Navigation Design

**Date:** 2026-05-16
**Branch:** feature-add-history-and-map
**Status:** Approved

## Summary

Replace the ad-hoc navigation links scattered across fragments with a persistent `BottomNavigationView` visible on all three screens: Track, Map, and History. Remove the top toolbar and FAB, which are unused placeholders.

## Decisions

| Decision | Choice | Reason |
|---|---|---|
| Nav style | Material `BottomNavigationView` (icons + labels) | Standard Android pattern, familiar UX |
| Top toolbar | Remove | Blank placeholder, wastes vertical space; each fragment has its own header |
| FAB | Remove | Email icon placeholder from template, no logic attached |
| Back behavior | Standard NavigationUI | Tabs are single-screen destinations; no per-tab back stacks needed |

## Visual Design

- Active tab: lime `#C6FF00` icon + label
- Inactive tabs: muted `#555566`
- Bar background: `#111118` with a `#1C1C22` top border
- Tab labels: Track · Map · History

## Changes by File

### `app/src/main/res/layout/activity_main.xml`

- Replace the root `CoordinatorLayout` (only needed for AppBar scroll coordination) with a vertical `LinearLayout`
- Remove `AppBarLayout` + `MaterialToolbar`
- Remove `FloatingActionButton`
- Keep `<include layout="@layout/content_main"/>` with `layout_weight="1"` so it fills remaining space
- Add `BottomNavigationView` below the include:
  - `android:id="@+id/bottom_nav"`
  - `app:menu="@menu/bottom_nav_menu"`
  - `app:itemIconTint` and `app:itemTextColor` set to a color selector: lime `#C6FF00` when selected, muted `#555566` otherwise

### `app/src/main/res/menu/bottom_nav_menu.xml` *(new file)*

Three items whose `android:id` values match the fragment IDs in the nav graph:

```xml
<item android:id="@+id/TrackingFragment" android:icon="@drawable/ic_nav_track" android:title="Track" />
<item android:id="@+id/MapFragment"      android:icon="@drawable/ic_nav_map"   android:title="Map" />
<item android:id="@+id/HistoryFragment"  android:icon="@drawable/ic_nav_history" android:title="History" />
```

Icons: add three vector drawable files to `res/drawable/`:
- `ic_nav_track.xml` — timer / stopwatch icon
- `ic_nav_map.xml` — map icon
- `ic_nav_history.xml` — list / history icon

Use `File > New > Vector Asset` in Android Studio (Material Symbols: `timer`, `map`, `format_list_bulleted`) or copy from the Material Icons set.

### `app/src/main/res/navigation/nav_graph.xml`

- Remove all `<action>` elements — tab switching is handled by `BottomNavigationView`, not directed actions
- All three fragments remain top-level destinations
- Start destination stays `TrackingFragment`

### `app/src/main/res/layout/content_main.xml`

- Remove `app:layout_behavior="@string/appbar_scrolling_view_behavior"` from the root `ConstraintLayout` (only relevant when inside a `CoordinatorLayout` with an AppBar)

### `app/src/main/java/com/runner/MainActivity.kt`

- Remove `setSupportActionBar()` and any toolbar binding
- After `setContentView`, wire navigation:
  ```kotlin
  val navController = supportFragmentManager
      .findFragmentById(R.id.nav_host_fragment_content_main)!!
      .findNavController()
  val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
  NavigationUI.setupWithNavController(bottomNav, navController)
  ```

### `app/src/main/res/layout/fragment_tracking.xml`

- Remove the bottom `LinearLayout` containing `textButtonMap` and `textButtonHistory`

### `app/src/main/java/com/runner/ui/tracking/TrackingFragment.kt`

- Remove click listeners for `textButtonMap` and `textButtonHistory`
- Remove the `findNavController().navigate(...)` calls for those buttons

### `app/src/main/java/com/runner/ui/history/HistoryFragment.kt`

- Remove any back-to-tracking navigation button and its click listener (if present)

### `app/src/main/java/com/runner/ui/map/MapFragment.kt`

- No changes needed

## Back Stack Behavior

Pressing back from Map or History returns to Tracking (start destination). Pressing back from Tracking exits the app. This is the default `NavigationUI` behavior with no extra configuration.

## Out of Scope

- Per-run detail screen (`RunActivity`) — existing behavior unchanged
- Map content / OSMDroid logic — unchanged
- Tracking logic / ViewModel — unchanged
