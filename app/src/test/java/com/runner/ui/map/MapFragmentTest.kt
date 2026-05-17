package com.runner.ui.map

import android.location.Location
import android.view.View
import org.junit.Assert.assertTrue
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
}
