package com.runner.ui.tracking

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.runner.MainActivity
import com.runner.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackingFlowTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun startButton_click_showsRunningState() {
        onView(withId(R.id.buttonStart)).perform(click())
        onView(withId(R.id.textStatusLabel)).check(matches(withText("RUNNING")))
        onView(withId(R.id.buttonStop)).check(matches(isDisplayed()))
        onView(withId(R.id.rowStartResume)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.buttonSave)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun stopButton_click_afterStart_showsStoppedState() {
        onView(withId(R.id.buttonStart)).perform(click())
        Thread.sleep(1100) // wait for the 1-second timer tick so elapsedSeconds > 0
        onView(withId(R.id.buttonStop)).perform(click())
        onView(withId(R.id.textStatusLabel)).check(matches(withText("STOPPED")))
        onView(withId(R.id.buttonResume)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonSave)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonStop)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun saveButton_click_afterStop_resetsToIdleState() {
        onView(withId(R.id.buttonStart)).perform(click())
        Thread.sleep(1100) // wait for timer tick so STOPPED state is reachable
        onView(withId(R.id.buttonStop)).perform(click())
        onView(withId(R.id.buttonSave)).perform(click())
        onView(withId(R.id.textStatusLabel)).check(matches(withText("IDLE")))
        onView(withId(R.id.textTimerDisplay)).check(matches(withText("00:00:00")))
        onView(withId(R.id.buttonSave)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.buttonResume)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun resumeButton_click_afterStop_returnsToRunningState() {
        onView(withId(R.id.buttonStart)).perform(click())
        Thread.sleep(1100) // wait for timer tick so resumeTracking() doesn't short-circuit
        onView(withId(R.id.buttonStop)).perform(click())
        onView(withId(R.id.buttonResume)).perform(click())
        onView(withId(R.id.textStatusLabel)).check(matches(withText("RUNNING")))
        onView(withId(R.id.buttonStop)).check(matches(isDisplayed()))
    }
}
