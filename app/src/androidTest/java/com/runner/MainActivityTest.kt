package com.runner

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun activity_launches_showing_trackingFragment() {
        onView(withId(R.id.textTimerDisplay)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNav_clickHistory_showsHistoryFragment() {
        onView(withId(R.id.HistoryFragment)).perform(click())
        onView(withId(R.id.recyclerViewHistory)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNav_clickMap_showsMapFragment() {
        onView(withId(R.id.MapFragment)).perform(click())
        onView(withId(R.id.mapView)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNav_returnToTracking_afterHistory_showsTrackingFragment() {
        onView(withId(R.id.HistoryFragment)).perform(click())
        onView(withId(R.id.TrackingFragment)).perform(click())
        onView(withId(R.id.textTimerDisplay)).check(matches(isDisplayed()))
    }
}
