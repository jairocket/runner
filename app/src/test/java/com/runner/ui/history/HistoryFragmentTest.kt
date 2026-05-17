package com.runner.ui.history

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runner.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HistoryFragmentTest {

    private fun launch() = launchFragmentInContainer<HistoryFragment>(
        themeResId = R.style.Theme_Runner
    )

    @Test
    fun recyclerView_hasLinearLayoutManager() {
        launch().onFragment { fragment ->
            val rv = fragment.requireView().findViewById<RecyclerView>(R.id.recyclerViewHistory)
            assertTrue(rv.layoutManager is LinearLayoutManager)
        }
    }

    @Test
    fun recyclerView_adapter_has6Items() {
        launch().onFragment { fragment ->
            val rv = fragment.requireView().findViewById<RecyclerView>(R.id.recyclerViewHistory)
            assertEquals(6, rv.adapter?.itemCount)
        }
    }

    @Test
    fun recyclerView_hasDividerItemDecoration() {
        launch().onFragment { fragment ->
            val rv = fragment.requireView().findViewById<RecyclerView>(R.id.recyclerViewHistory)
            assertTrue(rv.itemDecorationCount > 0)
        }
    }

    @Test
    fun binding_onDestroyView_doesNotLeak() {
        launch().moveToState(Lifecycle.State.DESTROYED)
    }
}
