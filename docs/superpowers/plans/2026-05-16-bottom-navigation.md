# Bottom Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace scattered per-fragment navigation links with a persistent Material `BottomNavigationView` wired to the NavController, visible across all three screens (Track, Map, History).

**Architecture:** A `BottomNavigationView` is added to `activity_main.xml` and connected to the existing `NavController` via `NavigationUI.setupWithNavController()`. All three fragments become peer top-level destinations in the nav graph; the nav bar handles switching. The toolbar and FAB are removed; fragment-level nav buttons are deleted.

**Tech Stack:** Kotlin, AndroidX Navigation KTX 2.6.0, Material Components (`BottomNavigationView`), Robolectric 4.11.1 (tests via `./run_tests.sh`)

---

## File Map

| Action | File |
|---|---|
| Create | `app/src/main/res/drawable/ic_nav_track.xml` |
| Create | `app/src/main/res/drawable/ic_nav_map.xml` |
| Create | `app/src/main/res/drawable/ic_nav_history.xml` |
| Create | `app/src/main/res/color/bottom_nav_color.xml` |
| Create | `app/src/main/res/menu/bottom_nav_menu.xml` |
| Create | `app/src/test/java/com/runner/ui/history/HistoryFragmentTest.kt` |
| Modify | `app/src/main/res/layout/activity_main.xml` |
| Modify | `app/src/main/res/layout/content_main.xml` |
| Modify | `app/src/main/res/navigation/nav_graph.xml` |
| Modify | `app/src/main/java/com/runner/MainActivity.kt` |
| Modify | `app/src/main/res/layout/fragment_tracking.xml` |
| Modify | `app/src/main/java/com/runner/ui/tracking/TrackingFragment.kt` |
| Modify | `app/src/main/res/layout/fragment_second.xml` |
| Modify | `app/src/main/java/com/runner/ui/history/HistoryFragment.kt` |
| Modify | `app/src/test/java/com/runner/ui/tracking/TrackingFragmentTest.kt` |

---

### Task 1: Add vector drawable icons for nav bar

**Files:**
- Create: `app/src/main/res/drawable/ic_nav_track.xml`
- Create: `app/src/main/res/drawable/ic_nav_map.xml`
- Create: `app/src/main/res/drawable/ic_nav_history.xml`

- [ ] **Step 1: Create `ic_nav_track.xml` (timer/stopwatch icon)**

```xml
<!-- app/src/main/res/drawable/ic_nav_track.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M15,1L9,1v2h6L15,1zM11,14h2L13,8h-2v6zM19.03,7.39l1.42,-1.42c-0.43,-0.51 -0.9,-0.99 -1.41,-1.41l-1.42,1.42C16.07,4.74 14.12,4 12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9 9,-4.03 9,-9c0,-2.12 -0.74,-4.07 -1.97,-5.61zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z"/>
</vector>
```

- [ ] **Step 2: Create `ic_nav_map.xml` (map/fold icon)**

```xml
<!-- app/src/main/res/drawable/ic_nav_map.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M20.5,3l-0.16,0.03L15,5.1 9,3 3.36,4.9c-0.21,0.07 -0.36,0.25 -0.36,0.48V20.5c0,0.28 0.22,0.5 0.5,0.5l0.16,-0.03L9,18.9l6,2.1 5.64,-1.9c0.21,-0.07 0.36,-0.25 0.36,-0.48V3.5c0,-0.28 -0.22,-0.5 -0.5,-0.5zM15,19l-6,-2.11V5l6,2.11V19z"/>
</vector>
```

- [ ] **Step 3: Create `ic_nav_history.xml` (list icon)**

```xml
<!-- app/src/main/res/drawable/ic_nav_history.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M3,13h2v-2L3,11v2zM3,17h2v-2L3,15v2zM3,9h2L5,7 3,7v2zM7,13h14v-2L7,11v2zM7,17h14v-2L7,15v2zM7,7v2h14L21,7 7,7z"/>
</vector>
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/drawable/ic_nav_track.xml \
        app/src/main/res/drawable/ic_nav_map.xml \
        app/src/main/res/drawable/ic_nav_history.xml
git commit -m "feat: add vector drawable icons for bottom navigation"
```

---

### Task 2: Add color selector and menu resource

**Files:**
- Create: `app/src/main/res/color/bottom_nav_color.xml`
- Create: `app/src/main/res/menu/bottom_nav_menu.xml`

- [ ] **Step 1: Create color state list**

```xml
<!-- app/src/main/res/color/bottom_nav_color.xml -->
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="#C6FF00" android:state_checked="true"/>
    <item android:color="#555566"/>
</selector>
```

- [ ] **Step 2: Create bottom nav menu**

The `android:id` values **must exactly match** the fragment IDs in `nav_graph.xml` (`TrackingFragment`, `MapFragment`, `HistoryFragment`) — this is how `NavigationUI` knows which tab maps to which destination.

```xml
<!-- app/src/main/res/menu/bottom_nav_menu.xml -->
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/TrackingFragment"
        android:icon="@drawable/ic_nav_track"
        android:title="Track"/>
    <item
        android:id="@+id/MapFragment"
        android:icon="@drawable/ic_nav_map"
        android:title="Map"/>
    <item
        android:id="@+id/HistoryFragment"
        android:icon="@drawable/ic_nav_history"
        android:title="History"/>
</menu>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/color/bottom_nav_color.xml \
        app/src/main/res/menu/bottom_nav_menu.xml
git commit -m "feat: add bottom nav color selector and menu resource"
```

---

### Task 3: Write failing tests

**Files:**
- Modify: `app/src/test/java/com/runner/ui/tracking/TrackingFragmentTest.kt`
- Create: `app/src/test/java/com/runner/ui/history/HistoryFragmentTest.kt`

- [ ] **Step 1: Add nav-link-absent test to `TrackingFragmentTest.kt`**

Add this test inside the existing `TrackingFragmentTest` class (after the last existing `@Test`):

```kotlin
@Test
fun `nav links are absent from layout`() {
    launch().onFragment { fragment ->
        assertNull(fragment.view?.findViewById<View>(R.id.textButtonMap))
        assertNull(fragment.view?.findViewById<View>(R.id.textButtonHistory))
    }
}
```

Also add this import at the top of the file if not already present:
```kotlin
import android.view.View
```

- [ ] **Step 2: Create `HistoryFragmentTest.kt`**

```kotlin
package com.runner.ui.history

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runner.R
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HistoryFragmentTest {

    @Test
    fun `back button is absent from layout`() {
        launchFragmentInContainer<HistoryFragment>(
            themeResId = R.style.Theme_Runner
        ).onFragment { fragment ->
            assertNull(fragment.view?.findViewById<View>(R.id.buttonSecond))
        }
    }
}
```

- [ ] **Step 3: Run tests and confirm they fail**

```bash
./run_tests.sh --tests "com.runner.ui.tracking.TrackingFragmentTest"
./run_tests.sh --tests "com.runner.ui.history.HistoryFragmentTest"
```

Expected: both tests **FAIL** — the views still exist in the layouts at this point.

- [ ] **Step 4: Commit the failing tests**

```bash
git add app/src/test/java/com/runner/ui/tracking/TrackingFragmentTest.kt \
        app/src/test/java/com/runner/ui/history/HistoryFragmentTest.kt
git commit -m "test: add failing tests for nav link removal"
```

---

### Task 4: Restructure `activity_main.xml`

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

Replace the entire file. This removes `CoordinatorLayout` (only useful for AppBar scroll coordination), removes `AppBarLayout`/toolbar/FAB, and adds `BottomNavigationView` below the nav host.

- [ ] **Step 1: Replace `activity_main.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:fitsSystemWindows="true"
    tools:context=".MainActivity">

    <include
        layout="@layout/content_main"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#111118"
        app:itemIconTint="@color/bottom_nav_color"
        app:itemTextColor="@color/bottom_nav_color"
        app:menu="@menu/bottom_nav_menu"/>

</LinearLayout>
```

---

### Task 5: Update `content_main.xml`

**Files:**
- Modify: `app/src/main/res/layout/content_main.xml`

Remove `app:layout_behavior="@string/appbar_scrolling_view_behavior"` — this attribute only has meaning inside a `CoordinatorLayout` with an `AppBarLayout`. Leaving it is harmless but misleading.

- [ ] **Step 1: Replace `content_main.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <fragment
        android:id="@+id/nav_host_fragment_content_main"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

### Task 6: Wire `NavigationUI` in `MainActivity`

**Files:**
- Modify: `app/src/main/java/com/runner/MainActivity.kt`

Remove toolbar setup. Wire `BottomNavigationView` to `NavController` using the `setupWithNavController` extension. The location tracking logic is unchanged.

- [ ] **Step 1: Replace `MainActivity.kt`**

```kotlin
package com.runner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.runner.ui.tracking.LocationViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val locationViewModel: LocationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navHostFragment.navController)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    locationViewModel.updateLocation(location)
                }
            }
        }

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
        } else {
            startTracking()
        }
    }

    private fun startTracking() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStart()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startTracking()
        }
    }
}
```

- [ ] **Step 2: Build to confirm no compile errors**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit layout and wiring changes**

```bash
git add app/src/main/res/layout/activity_main.xml \
        app/src/main/res/layout/content_main.xml \
        app/src/main/java/com/runner/MainActivity.kt
git commit -m "feat: replace toolbar/FAB with BottomNavigationView, wire NavigationUI"
```

---

### Task 7: Update `nav_graph.xml`

**Files:**
- Modify: `app/src/main/res/navigation/nav_graph.xml`

Remove all `<action>` elements. The nav bar handles tab switching; no directed actions between top-level destinations are needed.

- [ ] **Step 1: Replace `nav_graph.xml`**

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
        tools:layout="@layout/fragment_second"/>

    <fragment
        android:id="@+id/MapFragment"
        android:name="com.runner.ui.map.MapFragment"
        android:label="Map"
        tools:layout="@layout/fragment_map"/>

</navigation>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/navigation/nav_graph.xml
git commit -m "refactor: remove directed actions from nav graph, tabs are top-level destinations"
```

---

### Task 8: Remove nav links from `TrackingFragment`

**Files:**
- Modify: `app/src/main/res/layout/fragment_tracking.xml`
- Modify: `app/src/main/java/com/runner/ui/tracking/TrackingFragment.kt`

- [ ] **Step 1: Remove the bottom nav LinearLayout from `fragment_tracking.xml`**

Delete the entire block at the bottom of the file (lines 268–299 in the current file):

```xml
<!-- DELETE this entire LinearLayout and everything inside it -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:paddingTop="8dp"
    android:paddingBottom="24dp">

    <TextView
        android:id="@+id/textButtonMap"
        ... />

    <TextView
        android:id="@+id/textButtonHistory"
        ... />

</LinearLayout>
```

- [ ] **Step 2: Remove click listeners from `TrackingFragment.kt`**

Delete these lines from `onViewCreated`:

```kotlin
// DELETE both of these blocks:
binding.textButtonMap.setOnClickListener {
    findNavController().navigate(R.id.action_TrackingFragment_to_MapFragment)
}
binding.textButtonHistory.setOnClickListener {
    findNavController().navigate(R.id.action_TrackingFragment_to_HistoryFragment)
}
```

Also remove the now-unused import:

```kotlin
// DELETE this import if no other navigate() call remains in the file:
import androidx.navigation.fragment.findNavController
```

- [ ] **Step 3: Run the TrackingFragment test**

```bash
./run_tests.sh --tests "com.runner.ui.tracking.TrackingFragmentTest"
```

Expected: all tests **PASS**, including `nav links are absent from layout`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout/fragment_tracking.xml \
        app/src/main/java/com/runner/ui/tracking/TrackingFragment.kt
git commit -m "refactor: remove nav text links from TrackingFragment"
```

---

### Task 9: Remove back button from `HistoryFragment`

**Files:**
- Modify: `app/src/main/res/layout/fragment_second.xml`
- Modify: `app/src/main/java/com/runner/ui/history/HistoryFragment.kt`

- [ ] **Step 1: Remove `buttonSecond` from `fragment_second.xml`**

Delete the `TextView` with `android:id="@+id/buttonSecond"` from the header `LinearLayout`:

```xml
<!-- DELETE this TextView: -->
<TextView
    android:id="@+id/buttonSecond"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="&#8249; BACK"
    android:textColor="@color/colorTextMuted"
    android:textSize="13sp"
    android:letterSpacing="0.2"
    android:fontFamily="monospace"
    android:paddingStart="8dp"
    android:paddingEnd="0dp"/>
```

- [ ] **Step 2: Remove click listener from `HistoryFragment.kt`**

Delete these lines from `onViewCreated`:

```kotlin
// DELETE:
binding.buttonSecond.setOnClickListener {
    findNavController().navigate(R.id.action_HistoryFragment_to_TrackingFragment)
}
```

Also remove now-unused imports:

```kotlin
// DELETE both if no other usage remains:
import androidx.navigation.fragment.findNavController
import com.runner.R
```

- [ ] **Step 3: Run the HistoryFragment test**

```bash
./run_tests.sh --tests "com.runner.ui.history.HistoryFragmentTest"
```

Expected: **PASS** — `buttonSecond` is no longer in the layout.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout/fragment_second.xml \
        app/src/main/java/com/runner/ui/history/HistoryFragment.kt
git commit -m "refactor: remove back button from HistoryFragment"
```

---

### Task 10: Final build and full test run

- [ ] **Step 1: Run all unit tests**

```bash
./run_tests.sh
```

Expected: all tests **PASS** with no failures.

- [ ] **Step 2: Build release APK to confirm no lint or compile issues**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Smoke test on device or emulator**

- Launch the app — Track tab is active by default
- Tap Map tab — map screen appears, Map icon/label turns lime
- Tap History tab — run list appears, History icon/label turns lime
- Tap Track tab — tracking screen returns, Track icon/label turns lime
- Press back from Map — goes to Track tab, then back press exits the app
- Confirm toolbar and FAB are gone on all screens
