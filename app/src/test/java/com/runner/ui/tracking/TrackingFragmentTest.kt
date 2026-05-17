package com.runner.ui.tracking

import android.location.Location
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.button.MaterialButton
import com.runner.R
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class TrackingFragmentTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private fun launch() = launchFragmentInContainer<TrackingFragment>(
        themeResId = R.style.Theme_Runner
    )

    @Test
    fun timerDisplay_initialState_showsZero() {
        val scenario = launch()
        scenario.onFragment { fragment ->
            val text = fragment.requireView()
                .findViewById<TextView>(R.id.textTimerDisplay).text.toString()
            assertEquals("00:00:00", text)
        }
    }

    @Test
    fun paceValue_initialState_showsPlaceholder() {
        val scenario = launch()
        scenario.onFragment { fragment ->
            val text = fragment.requireView()
                .findViewById<TextView>(R.id.textPaceValue).text.toString()
            assertEquals("--:--", text)
        }
    }

    @Test
    fun distanceValue_initialState_showsZero() {
        val scenario = launch()
        scenario.onFragment { fragment ->
            val text = fragment.requireView()
                .findViewById<TextView>(R.id.textDistanceValue).text.toString()
            assertEquals("0.00", text)
        }
    }

    @Test
    fun startButton_initialState_isEnabled() {
        val scenario = launch()
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.buttonStart)
            assertTrue(btn.isEnabled)
        }
    }

    @Test
    fun stopButton_initialState_isHidden() {
        val scenario = launch()
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.buttonStop)
            assertEquals(View.GONE, btn.visibility)
        }
    }

    @Test
    fun binding_onDestroyView_doesNotLeak() {
        val scenario = launch()
        scenario.moveToState(Lifecycle.State.DESTROYED)
    }

    private fun getViewModel(fragment: TrackingFragment): LocationViewModel =
        ViewModelProvider(fragment.requireActivity())[LocationViewModel::class.java]

    // ── LiveData observers → UI ───────────────────────────────────────────────

    @Test
    fun elapsedSeconds_observer_formatsHoursMinutesSeconds() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(3661)) // 1h 1m 1s
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(
                "01:01:01",
                fragment.requireView().findViewById<TextView>(R.id.textTimerDisplay).text.toString()
            )
        }
    }

    @Test
    fun distanceKm_observer_updatesDistanceDisplay() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
            viewModel.updateLocation(Location("test").apply { latitude = 0.009; longitude = 0.0 })
            assertNotEquals(
                "0.00",
                fragment.requireView().findViewById<TextView>(R.id.textDistanceValue).text.toString()
            )
        }
    }

    @Test
    fun paceSecPerKm_observer_withValue_displaysMmSsFormat() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(600))
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.updateLocation(Location("test").apply { latitude = 0.0; longitude = 0.0 })
            viewModel.updateLocation(Location("test").apply { latitude = 0.009; longitude = 0.0 })
            val text = fragment.requireView().findViewById<TextView>(R.id.textPaceValue).text.toString()
            assertNotEquals("--:--", text)
            assertTrue(text.matches(Regex("\\d{2}:\\d{2}")))
        }
    }

    // ── applyTrackingState — RUNNING ──────────────────────────────────────────

    @Test
    fun startButton_click_showsRunningState() {
        launch().onFragment { fragment ->
            fragment.requireView().findViewById<MaterialButton>(R.id.buttonStart).performClick()
            val root = fragment.requireView()
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.buttonStop).visibility)
            assertEquals(View.GONE,    root.findViewById<View>(R.id.rowStartResume).visibility)
            assertEquals(View.GONE,    root.findViewById<View>(R.id.buttonSave).visibility)
            assertEquals("RUNNING",    root.findViewById<TextView>(R.id.textStatusLabel).text)
        }
    }

    // ── applyTrackingState — STOPPED ──────────────────────────────────────────

    @Test
    fun stopTracking_withElapsedTime_showsStoppedState() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.stopTracking()
            val root = fragment.requireView()
            assertEquals(View.GONE,    root.findViewById<View>(R.id.buttonStop).visibility)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.buttonResume).visibility)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.buttonSave).visibility)
            assertEquals("STOPPED",    root.findViewById<TextView>(R.id.textStatusLabel).text)
        }
    }

    @Test
    fun resumeButton_click_afterStop_transitionsToRunningState() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.stopTracking()
            fragment.requireView().findViewById<MaterialButton>(R.id.buttonResume).performClick()
            val root = fragment.requireView()
            assertEquals("RUNNING",    root.findViewById<TextView>(R.id.textStatusLabel).text)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.buttonStop).visibility)
        }
    }

    // ── applyTrackingState — IDLE (after save) ────────────────────────────────

    @Test
    fun saveButton_click_resetsToIdleState() {
        launch().onFragment { fragment ->
            val viewModel = getViewModel(fragment)
            viewModel.startTracking()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.stopTracking()
            fragment.requireView().findViewById<MaterialButton>(R.id.buttonSave).performClick()
            val root = fragment.requireView()
            assertEquals("00:00:00", root.findViewById<TextView>(R.id.textTimerDisplay).text)
            assertEquals("IDLE",     root.findViewById<TextView>(R.id.textStatusLabel).text)
            assertEquals(View.GONE,  root.findViewById<View>(R.id.buttonResume).visibility)
            assertEquals(View.GONE,  root.findViewById<View>(R.id.buttonStop).visibility)
        }
    }

}
