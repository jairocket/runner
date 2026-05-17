# Map Initial Centering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Center the map on the device's current location when MapFragment opens, showing a loading spinner until a GPS fix is available.

**Architecture:** Add a full-screen overlay to `fragment_map.xml` that sits above the `MapView`. `MapFragment` checks at startup whether a location is already available (from `locationHistory` or `locationLiveData.value`) and hides the overlay immediately; if not, a `locationLiveData` observer hides it on the first incoming fix. A `hasInitialCenter` flag prevents re-centering after the first fix.

**Tech Stack:** Kotlin, OSMDroid 6.1.18, Material Components (`CircularProgressIndicator`), AndroidX View Binding, Jetpack LiveData.

---

### Task 1: Add loading overlay to `fragment_map.xml`

**Files:**
- Modify: `app/src/main/res/layout/fragment_map.xml`

- [ ] **Step 1: Add the overlay `FrameLayout` after the existing `MaterialButton`**

Replace the entire file content with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/colorBackground">

    <org.osmdroid.views.MapView
        android:id="@+id/mapView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

    <com.google.android.material.button.MaterialButton
        android:id="@+id/buttonMapBack"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="top|start"
        android:layout_margin="16dp"
        android:text="‹ BACK"
        android:textColor="#000000"
        app:backgroundTint="@color/colorAccentLime"
        app:cornerRadius="2dp"/>

    <FrameLayout
        android:id="@+id/locationLoadingOverlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/colorBackground"
        android:visibility="visible">

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:gravity="center"
            android:orientation="vertical"
            android:padding="24dp">

            <com.google.android.material.progressindicator.CircularProgressIndicator
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:indeterminate="true"/>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="Getting your location…"
                android:textSize="14sp"/>

        </LinearLayout>

    </FrameLayout>

</FrameLayout>
```

- [ ] **Step 2: Build to confirm the layout compiles**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

---

### Task 2: Update `MapFragment.kt` with initial centering logic

**Files:**
- Modify: `app/src/main/java/com/runner/ui/map/MapFragment.kt`

> **Note on unit tests:** OSMDroid's `MapView` initialises a tile provider and file-system cache at construction time, which makes it crash inside Robolectric without custom shadows. There is no existing `MapFragmentTest` in this project. The correct verification for this task is a build + manual device test (Task 3).

- [ ] **Step 1: Replace the full file content**

```kotlin
package com.runner.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.runner.databinding.FragmentMapBinding
import com.runner.ui.tracking.LocationViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline

@Suppress("DEPRECATION")
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LocationViewModel by activityViewModels()
    private val routePolyline = Polyline()
    private var hasInitialCenter = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(
            requireContext(),
            android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            overlays.add(routePolyline)
        }

        drawHistory()

        when {
            viewModel.locationHistory.isNotEmpty() -> hideOverlay()
            viewModel.locationLiveData.value != null -> {
                val loc = viewModel.locationLiveData.value!!
                binding.mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                hideOverlay()
            }
        }

        viewModel.locationLiveData.observe(viewLifecycleOwner) { location ->
            if (!hasInitialCenter) {
                binding.mapView.controller.setCenter(GeoPoint(location.latitude, location.longitude))
                hideOverlay()
            }
            if (viewModel.isTracking.value == true) {
                val point = GeoPoint(location.latitude, location.longitude)
                routePolyline.addPoint(point)
                binding.mapView.controller.animateTo(point)
                binding.mapView.invalidate()
            }
        }

        binding.buttonMapBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun hideOverlay() {
        binding.locationLoadingOverlay.visibility = View.GONE
        hasInitialCenter = true
    }

    private fun drawHistory() {
        val points = viewModel.locationHistory.map { GeoPoint(it.latitude, it.longitude) }
        routePolyline.setPoints(points)
        if (points.isNotEmpty()) {
            binding.mapView.controller.animateTo(points.last())
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

- [ ] **Step 2: Build to confirm it compiles**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

---

### Task 3: Install, run, and verify on device

**Files:** none

- [ ] **Step 1: Install and launch**

```bash
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.runner/.MainActivity
```

Expected: app opens on the tracking screen.

- [ ] **Step 2: Open the map**

Tap the MAP button. Verify:
1. A spinner with "Getting your location…" appears briefly (if GPS fix not yet cached).
2. The map snaps to the device's actual location (Brazil), not (0, 0) / western Africa.
3. Map tiles render correctly (not all blue).
4. The BACK button returns to the tracking screen.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/fragment_map.xml \
        app/src/main/java/com/runner/ui/map/MapFragment.kt
git commit -m "$(cat <<'EOF'
feat(): Center map on device location with GPS loading overlay

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```
