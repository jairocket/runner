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
    fun updateLocation_withRealMovement_derivesSpeedFromPosition() {
        viewModel.startTracking()
        // ~11.1 m north in 5 s = 2.22 m/s = 7.99 km/h
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L })
        viewModel.updateLocation(Location("test").apply { latitude = 0.0001; longitude = 0.0; time = 5000L })
        assertEquals(8.0f, viewModel.speedKmh.value!!, 0.2f)
    }

    @Test
    fun updateLocation_withoutGpsSpeed_speedKmhIsNull() {
        val location = Location("test").apply { latitude = 0.0; longitude = 0.0 }
        viewModel.updateLocation(location)
        assertNull(viewModel.speedKmh.value)
    }

    @Test
    fun updateLocation_whenNotTracking_doesNotSetSpeedKmh() {
        val location = Location("test").apply {
            latitude = 0.0; longitude = 0.0
            speed = 10.0f
        }
        viewModel.updateLocation(location)
        assertNull(viewModel.speedKmh.value)
    }

    @Test
    fun updateLocation_withGpsDrift_speedKmhIsNull() {
        viewModel.startTracking()
        val loc1 = Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L }
        // ~1 m north in 5 s = 0.2 m/s implied; speed field set so hasSpeed()=true,
        // proving it is the velocity filter — not hasSpeed() — that produces null.
        val loc2 = Location("test").apply {
            latitude = 0.000009; longitude = 0.0; time = 5000L
            speed = 0.6f
        }
        viewModel.updateLocation(loc1)
        viewModel.updateLocation(loc2)
        assertNull(viewModel.speedKmh.value)
    }

    @Test
    fun stopTracking_resetsSpeedKmhToNull() {
        viewModel.startTracking()
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L })
        viewModel.updateLocation(Location("test").apply { latitude = 0.0001; longitude = 0.0; time = 5000L })
        assertNotNull(viewModel.speedKmh.value)
        viewModel.stopTracking()
        assertNull(viewModel.speedKmh.value)
    }

    @Test
    fun resetTimer_resetsSpeedKmhToNull() {
        viewModel.startTracking()
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L })
        viewModel.updateLocation(Location("test").apply { latitude = 0.0001; longitude = 0.0; time = 5000L })
        assertNotNull(viewModel.speedKmh.value)
        viewModel.stopTracking()
        viewModel.resetTimer()
        assertNull(viewModel.speedKmh.value)
    }

    @Test
    fun updateLocation_withGpsDrift_doesNotAccumulateDistance() {
        viewModel.startTracking()
        val loc1 = Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L }
        // ~1 m north in 5 s = 0.2 m/s implied speed — typical GPS jitter when stationary
        val loc2 = Location("test").apply { latitude = 0.000009; longitude = 0.0; time = 5000L }
        viewModel.updateLocation(loc1)
        viewModel.updateLocation(loc2)
        assertEquals(0.0, viewModel.distanceKm.value!!, 0.0005)
    }

    @Test
    fun updateLocation_withVelocityBetween0_3And0_5MetersPerSecond_speedKmhIsNull() {
        viewModel.startTracking()
        // ~2 m north in 5 s = 0.4 m/s: above old 0.3 threshold, below new 0.5 threshold
        viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L })
        viewModel.updateLocation(Location("test").apply { latitude = 2.0 / 111111.0; longitude = 0.0; time = 5000L })
        assertNull(viewModel.speedKmh.value)
    }

    @Test
    fun updateLocation_withDriftThenOscillation_doesNotAccumulateDistance() {
        viewModel.startTracking()
        // loc1: anchor
        val loc1 = Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L }
        // loc2: ~2.4 m north in 5 s = 0.48 m/s — just below new 0.5 threshold, so filtered
        //   without anchoring: lastLocation moves to loc2
        //   with anchoring:    lastLocation stays at loc1
        val loc2 = Location("test").apply { latitude = 2.4 / 111111.0; longitude = 0.0; time = 5000L }
        // loc3: ~1 m south of origin — oscillation back
        //   without anchoring: delta from loc2 = 3.4 m / 5 s = 0.68 m/s → passes → distance accumulates
        //   with anchoring:    delta from loc1 = 1.0 m / 5 s = 0.20 m/s → filtered → distance stays 0
        val loc3 = Location("test").apply { latitude = -1.0 / 111111.0; longitude = 0.0; time = 10000L }
        viewModel.updateLocation(loc1)
        viewModel.updateLocation(loc2)
        viewModel.updateLocation(loc3)
        assertEquals(0.0, viewModel.distanceKm.value!!, 0.001)
    }

    @Test
    fun updateLocation_withLowAccuracyFix_isIgnoredAndDoesNotCorruptLastLocation() {
        viewModel.startTracking()
        val loc1 = Location("test").apply { latitude = 0.0; longitude = 0.0; time = 0L }
        // Low-accuracy fix far from real position — should be ignored entirely
        val lowAccuracy = Location("test").apply {
            latitude = 0.0009; longitude = 0.0; time = 5000L
            accuracy = 25f
        }
        // ~1 m north of origin in 10 s = 0.1 m/s — below threshold, should be filtered
        // Without gate: compared to lowAccuracy (~100 m away) → huge spurious speed
        // With gate:    compared to loc1 (origin) → 0.1 m/s → filtered → null
        val loc3 = Location("test").apply { latitude = 0.000009; longitude = 0.0; time = 10000L }
        viewModel.updateLocation(loc1)
        viewModel.updateLocation(lowAccuracy)
        viewModel.updateLocation(loc3)
        assertNull(viewModel.speedKmh.value)
        assertEquals(0.0, viewModel.distanceKm.value!!, 0.001)
    }
}
