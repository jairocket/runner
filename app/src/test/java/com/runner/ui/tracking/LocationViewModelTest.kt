package com.runner.ui.tracking

import android.location.Location
import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

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

    @Test
    fun resetTimer_clearsLocationHistory() {
        viewModel.startTracking()
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
        viewModel.stopTracking()
        viewModel.resetTimer()
        assertTrue(viewModel.locationHistory.isEmpty())
    }

    @Test
    fun resetTimer_emitsTrajectorySaved() {
        viewModel.startTracking()
        viewModel.stopTracking()
        var emitted = false
        viewModel.trajectorySaved.observeForever { emitted = true }
        viewModel.resetTimer()
        assertTrue(emitted)
    }

    // ── speedKmh ──────────────────────────────────────────────────────────────

    @Test
    fun speedKmh_initialValue_isNull() {
        assertNull(viewModel.speedKmh.value)
    }

    @Test
    fun updateLocation_withGpsSpeed_convertsToKmh() {
        val location = Location("test").apply {
            latitude = 0.0; longitude = 0.0
            speed = 10.0f // 10 m/s = 36 km/h
        }
        viewModel.updateLocation(location)
        assertEquals(36.0f, viewModel.speedKmh.value!!, 0.01f)
    }

    @Test
    fun updateLocation_withoutGpsSpeed_speedKmhIsNull() {
        val location = Location("test").apply { latitude = 0.0; longitude = 0.0 }
        viewModel.updateLocation(location)
        assertNull(viewModel.speedKmh.value)
    }
}
