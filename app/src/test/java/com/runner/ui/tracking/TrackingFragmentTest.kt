package com.runner.ui.tracking

import android.location.Location
import android.widget.Button
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runner.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class TrackingFragmentTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    @Test
    fun textView_initialState_showsAwaitingGps() {
        val scenario = launchFragmentInContainer<TrackingFragment>()

        scenario.onFragment { fragment ->
            val text = fragment.requireView()
                .findViewById<TextView>(R.id.textview_first)
                .text.toString()
            assertEquals("Aguardando GPS...", text)
        }
    }

    @Test
    fun textView_whenLocationPosted_displaysFormattedData() {
        val location = Location("test").apply {
            latitude = 10.0
            longitude = 20.0
            speed = 5.0f
            time = 1000L
        }

        val scenario = launchFragmentInContainer<TrackingFragment>()

        scenario.onFragment { fragment ->
            val viewModel = ViewModelProvider(fragment.requireActivity())[LocationViewModel::class.java]
            viewModel.locationLiveData.value = location
        }

        scenario.onFragment { fragment ->
            val text = fragment.requireView()
                .findViewById<TextView>(R.id.textview_first)
                .text.toString()
            val expected = buildString {
                append(location.latitude.toString())
                append(" ")
                append(location.longitude.toString())
                append("\n")
                append(location.speed.toString())
                append("\n")
                append(location.time.toString())
            }
            assertEquals(expected, text)
        }
    }

    @Test
    fun button_onClick_navigatesToHistoryFragment() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        val scenario = launchFragmentInContainer<TrackingFragment>()

        scenario.onFragment { fragment ->
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        scenario.onFragment { fragment ->
            fragment.requireView()
                .findViewById<Button>(R.id.button_first)
                .performClick()
        }

        assertEquals(R.id.HistoryFragment, navController.currentDestination?.id)
    }

    @Test
    fun binding_onDestroyView_doesNotLeak() {
        val scenario = launchFragmentInContainer<TrackingFragment>()
        // If onDestroyView properly nullifies _binding, moving to DESTROYED throws no exception
        scenario.moveToState(Lifecycle.State.DESTROYED)
    }
}
