package com.runner.ui.tracking

import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class LocationViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val viewModel = LocationViewModel()

    // ── locationLiveData ──────────────────────────────────────────────────────

    @Test
    fun locationLiveData_initialValue_isNull() {
        assertNull(viewModel.locationLiveData.value)
    }

    @Test
    fun locationLiveData_whenValuePosted_observerReceivesValue() {
        val location = Location("test").apply {
            latitude = 10.0
            longitude = 20.0
            speed = 5.0f
            time = 1000L
        }

        var received: Location? = null
        viewModel.locationLiveData.observeForever { received = it }

        viewModel.locationLiveData.value = location

        assertEquals(10.0, received?.latitude)
        assertEquals(20.0, received?.longitude)
        assertEquals(5.0f, received?.speed)
        assertEquals(1000L, received?.time)
    }

    // ── tracking state ────────────────────────────────────────────────────────

    @Test
    fun isTracking_initialValue_isFalse() {
        assertFalse(viewModel.isTracking.value!!)
    }

    @Test
    fun elapsedSeconds_initialValue_isZero() {
        assertEquals(0L, viewModel.elapsedSeconds.value)
    }

    @Test
    fun distanceKm_initialValue_isZero() {
        assertEquals(0.0, viewModel.distanceKm.value!!, 0.001)
    }

    @Test
    fun paceSecPerKm_initialValue_isNull() {
        assertNull(viewModel.paceSecPerKm.value)
    }

    @Test
    fun startTracking_setsIsTrackingTrue() {
        viewModel.startTracking()
        assertTrue(viewModel.isTracking.value!!)
    }

    @Test
    fun stopTracking_afterStart_setsIsTrackingFalse() {
        viewModel.startTracking()
        viewModel.stopTracking()
        assertFalse(viewModel.isTracking.value!!)
    }

    @Test
    fun startTracking_resetsMetrics() {
        viewModel.startTracking()
        viewModel.stopTracking()
        viewModel.startTracking()
        assertEquals(0.0, viewModel.distanceKm.value!!, 0.001)
        assertNull(viewModel.paceSecPerKm.value)
    }

    @Test
    fun updateLocation_whenNotTracking_doesNotAccumulateDistance() {
        val loc1 = Location("test").apply { latitude = 0.0; longitude = 0.0 }
        val loc2 = Location("test").apply { latitude = 0.001; longitude = 0.0 }
        viewModel.updateLocation(loc1)
        viewModel.updateLocation(loc2)
        assertEquals(0.0, viewModel.distanceKm.value!!, 0.001)
    }

    @Test
    fun updateLocation_updatesLocationLiveData() {
        val location = Location("test").apply { latitude = 5.0; longitude = 10.0 }
        viewModel.updateLocation(location)
        assertEquals(5.0, viewModel.locationLiveData.value?.latitude)
        assertEquals(10.0, viewModel.locationLiveData.value?.longitude)
    }
}
