package com.runner.ui.history

import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runner.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HistoryAdapterTest {

    private val context = RuntimeEnvironment.getApplication()

    private val sampleItems = listOf(
        RunActivity("1", "May 14, 2026", "42:17", "6.2 km", "6:49", emptyList()),
        RunActivity("2", "May 12, 2026", "31:04", "4.8 km", "6:28", emptyList())
    )

    private fun makeHolder(): HistoryAdapter.ViewHolder =
        HistoryAdapter(sampleItems) {}.onCreateViewHolder(FrameLayout(context), 0)

    @Test
    fun getItemCount_returnsListSize() {
        assertEquals(2, HistoryAdapter(sampleItems) {}.itemCount)
    }

    @Test
    fun getItemCount_emptyList_returnsZero() {
        assertEquals(0, HistoryAdapter(emptyList()) {}.itemCount)
    }

    @Test
    fun bind_displaysDate() {
        val adapter = HistoryAdapter(sampleItems) {}
        val holder = makeHolder()
        adapter.onBindViewHolder(holder, 0)
        assertEquals("May 14, 2026", holder.itemView.findViewById<TextView>(R.id.textItemDate).text.toString())
    }

    @Test
    fun bind_displaysDistance() {
        val adapter = HistoryAdapter(sampleItems) {}
        val holder = makeHolder()
        adapter.onBindViewHolder(holder, 0)
        assertEquals("6.2 km", holder.itemView.findViewById<TextView>(R.id.textItemDistance).text.toString())
    }

    @Test
    fun bind_displaysDuration() {
        val adapter = HistoryAdapter(sampleItems) {}
        val holder = makeHolder()
        adapter.onBindViewHolder(holder, 0)
        assertEquals("42:17", holder.itemView.findViewById<TextView>(R.id.textItemDuration).text.toString())
    }

    @Test
    fun bind_displaysPaceWithMinKmSuffix() {
        val adapter = HistoryAdapter(sampleItems) {}
        val holder = makeHolder()
        adapter.onBindViewHolder(holder, 0)
        assertEquals("6:49 min/km", holder.itemView.findViewById<TextView>(R.id.textItemPace).text.toString())
    }

    @Test
    fun bind_secondItem_displaysCorrectDate() {
        val adapter = HistoryAdapter(sampleItems) {}
        val holder = makeHolder()
        adapter.onBindViewHolder(holder, 1)
        assertEquals("May 12, 2026", holder.itemView.findViewById<TextView>(R.id.textItemDate).text.toString())
    }

    @Test
    fun clickingItem_invokesCallback_withCorrectRun() {
        var clicked: RunActivity? = null
        val adapter = HistoryAdapter(sampleItems) { run -> clicked = run }
        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.performClick()
        assertEquals(sampleItems[0], clicked)
    }
}
