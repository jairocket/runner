package com.runner.ui.tracking

import android.view.View
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.button.MaterialButton
import com.runner.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

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
    fun historyLink_onClick_navigatesToHistoryFragment() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        val scenario = launch()

        scenario.onFragment { fragment ->
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        scenario.onFragment { fragment ->
            fragment.requireView()
                .findViewById<TextView>(R.id.textButtonHistory)
                .performClick()
        }

        assertEquals(R.id.HistoryFragment, navController.currentDestination?.id)
    }

    @Test
    fun binding_onDestroyView_doesNotLeak() {
        val scenario = launch()
        scenario.moveToState(Lifecycle.State.DESTROYED)
    }
}
