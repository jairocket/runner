package com.runner.ui.history

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runner.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class RunDetailFragmentTest {

    private fun argsFor(runId: String) = Bundle().apply {
        putString(RunDetailFragment.ARG_RUN_ID, runId)
    }

    @Test
    fun statsAreDisplayed_forValidRun() {
        val run = MockRunRepository().getById("1")!!
        launchFragmentInContainer<RunDetailFragment>(
            fragmentArgs = argsFor("1"),
            themeResId = R.style.Theme_Runner
        ).onFragment { fragment ->
            val view = fragment.requireView()
            assertEquals(run.date, view.findViewById<TextView>(R.id.textDetailDate).text.toString())
            assertEquals(run.distanceKm, view.findViewById<TextView>(R.id.textDetailDistance).text.toString())
            assertEquals(run.duration, view.findViewById<TextView>(R.id.textDetailDuration).text.toString())
            assertEquals("${run.paceMinKm} min/km", view.findViewById<TextView>(R.id.textDetailPace).text.toString())
            val km = run.distanceKm.removeSuffix(" km").toDouble()
            val (mins, secs) = run.duration.split(":").map { it.toLong() }
            val hours = (mins * 60 + secs) / 3600.0
            val expectedSpeed = "${"%.1f".format(km / hours)} km/h"
            assertEquals(expectedSpeed, view.findViewById<TextView>(R.id.textDetailAvgSpeed).text.toString())
        }
    }

    @Test
    fun fragment_launchesWithoutCrash_forUnknownRunId() {
        launchFragmentInContainer<RunDetailFragment>(
            fragmentArgs = argsFor("999"),
            themeResId = R.style.Theme_Runner
        ).moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun binding_onDestroyView_doesNotLeak() {
        launchFragmentInContainer<RunDetailFragment>(
            fragmentArgs = argsFor("1"),
            themeResId = R.style.Theme_Runner
        ).moveToState(Lifecycle.State.DESTROYED)
    }
}
