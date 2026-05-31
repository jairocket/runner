# Integration Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Espresso instrumented integration tests covering the three main user flows: bottom nav switching, the full tracking start/stop/save lifecycle, and history list-to-detail navigation.

**Architecture:** Four new files under `app/src/androidTest/`: a `MainActivityTest` for the Activity shell and bottom nav, a `TrackingFlowTest` for the tracking lifecycle, and a `HistoryNavigationTest` for list→detail navigation. All use `ActivityScenarioRule<MainActivity>` + `GrantPermissionRule` to pre-grant location permission and avoid the system dialog blocking tests. `espresso-contrib` is added to enable `RecyclerViewActions` for clicking RecyclerView items.

**Tech Stack:** Espresso 3.5.1, espresso-contrib 3.5.1, `ActivityScenarioRule`, `GrantPermissionRule`, `RecyclerViewActions`, `AndroidJUnit4`

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Modify | `gradle/libs.versions.toml` | Add `espresso-contrib` library alias |
| Modify | `app/build.gradle.kts` | Wire `espresso-contrib` to `androidTestImplementation` |
| Create | `app/src/androidTest/java/com/runner/MainActivityTest.kt` | Launch + bottom nav switching |
| Create | `app/src/androidTest/java/com/runner/ui/tracking/TrackingFlowTest.kt` | Start / Stop / Save / Resume lifecycle |
| Create | `app/src/androidTest/java/com/runner/ui/history/HistoryNavigationTest.kt` | History list → RunDetailFragment navigation |

---

## Task 1: Add espresso-contrib dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

`RecyclerViewActions.actionOnItemAtPosition` (needed in Task 4 to click a RecyclerView row) lives in `espresso-contrib`. The version must match the existing `espressoCore = "3.5.1"` to avoid dependency conflicts.

- [ ] **Step 1: Write a placeholder that imports RecyclerViewActions (compile probe)**

Create `app/src/androidTest/java/com/runner/ui/history/HistoryNavigationTest.kt`:

```kotlin
package com.runner.ui.history

import androidx.test.espresso.contrib.RecyclerViewActions
import org.junit.Test

class HistoryNavigationTest {
    @Test fun placeholder() {}
}
```

- [ ] **Step 2: Run to confirm compilation fails**

```bash
./gradlew assembleAndroidTest 2>&1 | grep -i "error"
```

Expected: `error: unresolved reference: contrib` (or similar). This confirms the dependency is missing.

- [ ] **Step 3: Add the library alias to libs.versions.toml**

In `gradle/libs.versions.toml`, inside `[libraries]`, add after the existing espresso line:

```toml
androidx-espresso-contrib = { group = "androidx.test.espresso", name = "espresso-contrib", version.ref = "espressoCore" }
```

- [ ] **Step 4: Wire it in build.gradle.kts**

In `app/build.gradle.kts`, inside `dependencies { }`, add:

```kotlin
androidTestImplementation(libs.androidx.espresso.contrib)
```

- [ ] **Step 5: Confirm compilation now succeeds**

```bash
./gradlew assembleAndroidTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Delete the placeholder file**

Delete `app/src/androidTest/java/com/runner/ui/history/HistoryNavigationTest.kt` — the real version is written in Task 4.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "test: add espresso-contrib for RecyclerView integration tests"
```

---

## Task 2: MainActivityTest — launch and bottom navigation

**Files:**
- Create: `app/src/androidTest/java/com/runner/MainActivityTest.kt`

These tests verify that `MainActivity` starts on the Tracking screen, and that tapping each bottom nav item replaces the visible fragment content.

Background: Bottom nav item IDs (`R.id.TrackingFragment`, `R.id.MapFragment`, `R.id.HistoryFragment`) intentionally match the nav graph fragment IDs — that's how `setupWithNavController` maps taps to destinations. Espresso treats them as ordinary views.

- [ ] **Step 1: Write the failing tests**

Create `app/src/androidTest/java/com/runner/MainActivityTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Confirm it compiles**

```bash
./gradlew assembleAndroidTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run on a connected device**

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.runner.MainActivityTest
```

Expected: 4 tests PASS.

Note: `GrantPermissionRule` fields are evaluated before `ActivityScenarioRule` when declared first (JUnit 4 applies `@Rule` fields in the order they are declared). If the permission dialog still appears, wrap them in a `RuleChain`:

```kotlin
@get:Rule
val rules: RuleChain = RuleChain
    .outerRule(GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ))
    .around(ActivityScenarioRule(MainActivity::class.java))
```

And remove the individual `@get:Rule` declarations.

- [ ] **Step 4: Commit**

```bash
git add app/src/androidTest/java/com/runner/MainActivityTest.kt
git commit -m "test: add MainActivityTest for launch and bottom nav integration"
```

---

## Task 3: TrackingFlowTest — start, stop, resume, save lifecycle

**Files:**
- Create: `app/src/androidTest/java/com/runner/ui/tracking/TrackingFlowTest.kt`

These tests cover the four tracking state transitions driven entirely through button clicks.

Important: `TrackingFragment.applyTrackingState` only enters STOPPED state when `elapsedSeconds > 0`. The ViewModel timer ticks every 1 second (started via `handler.post(timerRunnable)`, then `postDelayed(1000)`). If Stop is clicked before the first tick, `elapsedSeconds` is still 0 and the fragment shows IDLE instead of STOPPED. The tests that need STOPPED state include a 1.1-second sleep between Start and Stop clicks to guarantee the timer has ticked at least once.

- [ ] **Step 1: Write the failing tests**

Create `app/src/androidTest/java/com/runner/ui/tracking/TrackingFlowTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Confirm it compiles**

```bash
./gradlew assembleAndroidTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run on a connected device**

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.runner.ui.tracking.TrackingFlowTest
```

Expected: 4 tests PASS. If any test using `withEffectiveVisibility` fails with a view-not-found error, replace `withEffectiveVisibility(Visibility.GONE)` with `not(isDisplayed())` — some Espresso versions require the view to be in the hierarchy for `withEffectiveVisibility` to match.

- [ ] **Step 4: Commit**

```bash
git add app/src/androidTest/java/com/runner/ui/tracking/TrackingFlowTest.kt
git commit -m "test: add TrackingFlowTest for start/stop/resume/save integration"
```

---

## Task 4: HistoryNavigationTest — list to detail

**Files:**
- Create: `app/src/androidTest/java/com/runner/ui/history/HistoryNavigationTest.kt`

These tests navigate to the History tab, verify items are shown, click the first item, and confirm `RunDetailFragment` renders the correct run data. The first item in `mockRuns` is id="1": date="May 14, 2026", distance="6.2 km".

`RecyclerViewActions.actionOnItemAtPosition<HistoryAdapter.ViewHolder>(0, click())` scrolls to position 0 and performs a click — it handles the scroll even if the item is off-screen.

- [ ] **Step 1: Write the failing tests**

Create `app/src/androidTest/java/com/runner/ui/history/HistoryNavigationTest.kt`:

```kotlin
package com.runner.ui.history

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.runner.MainActivity
import com.runner.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryNavigationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private fun navigateToHistory() {
        onView(withId(R.id.HistoryFragment)).perform(click())
    }

    @Test
    fun historyTab_showsRunList() {
        navigateToHistory()
        onView(withId(R.id.recyclerViewHistory)).check(matches(isDisplayed()))
    }

    @Test
    fun historyList_tapFirstItem_navigatesToRunDetailFragment() {
        navigateToHistory()
        onView(withId(R.id.recyclerViewHistory))
            .perform(RecyclerViewActions.actionOnItemAtPosition<HistoryAdapter.ViewHolder>(0, click()))
        onView(withId(R.id.textDetailDate)).check(matches(isDisplayed()))
    }

    @Test
    fun historyList_tapFirstItem_showsCorrectDate() {
        navigateToHistory()
        onView(withId(R.id.recyclerViewHistory))
            .perform(RecyclerViewActions.actionOnItemAtPosition<HistoryAdapter.ViewHolder>(0, click()))
        onView(withId(R.id.textDetailDate)).check(matches(withText("May 14, 2026")))
    }

    @Test
    fun historyList_tapFirstItem_showsCorrectDistance() {
        navigateToHistory()
        onView(withId(R.id.recyclerViewHistory))
            .perform(RecyclerViewActions.actionOnItemAtPosition<HistoryAdapter.ViewHolder>(0, click()))
        onView(withId(R.id.textDetailDistance)).check(matches(withText("6.2 km")))
    }
}
```

- [ ] **Step 2: Confirm it compiles**

```bash
./gradlew assembleAndroidTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run on a connected device**

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.runner.ui.history.HistoryNavigationTest
```

Expected: 4 tests PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/androidTest/java/com/runner/ui/history/HistoryNavigationTest.kt
git commit -m "test: add HistoryNavigationTest for list-to-detail navigation integration"
```

---

## Self-Review

**Spec coverage:**
- Bottom nav switching: ✓ Task 2
- Tracking start/stop/resume/save: ✓ Task 3
- History list → RunDetailFragment: ✓ Task 4
- MapFragment visible via bottom nav: ✓ Task 2 (`bottomNav_clickMap_showsMapFragment`)

**Placeholder scan:** No TBD, TODO, or vague steps. All test code is complete. The `Thread.sleep(1100)` calls are documented with the exact reason (timer-tick dependency).

**Type consistency:**
- `HistoryAdapter.ViewHolder` is an inner class confirmed in `HistoryAdapter.kt:13`
- View IDs (`textTimerDisplay`, `buttonStart`, `buttonStop`, `buttonResume`, `buttonSave`, `rowStartResume`, `textStatusLabel`, `recyclerViewHistory`, `mapView`, `textDetailDate`, `textDetailDistance`) all confirmed against layout XMLs
- Bottom nav item IDs (`R.id.TrackingFragment`, `R.id.MapFragment`, `R.id.HistoryFragment`) confirmed against `bottom_nav_menu.xml`
- Mock run[0] date="May 14, 2026" and distanceKm="6.2 km" confirmed against `MockRuns.kt`
