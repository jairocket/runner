package com.runner.ui.history

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runner.R
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HistoryFragmentTest {

    @Test
    fun `back button is absent from layout`() {
        launchFragmentInContainer<HistoryFragment>(
            themeResId = R.style.Theme_Runner
        ).onFragment { fragment ->
            assertNull(fragment.view?.findViewById<View>(R.id.buttonSecond))
        }
    }
}
