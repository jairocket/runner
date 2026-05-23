package com.runner.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryViewModelTest {

    private fun makeRepo(runs: List<RunActivity> = mockRuns): RunRepository =
        object : RunRepository {
            override fun getAll() = runs
            override fun getById(id: String) = runs.find { it.id == id }
        }

    @Test
    fun runs_returnsAllRunsFromRepo() {
        val vm = HistoryViewModel(makeRepo())
        assertEquals(6, vm.runs.size)
    }

    @Test
    fun runs_returnsCorrectFirstRun() {
        val vm = HistoryViewModel(makeRepo())
        assertEquals("May 14, 2026", vm.runs.first().date)
    }

    @Test
    fun runs_emptyRepo_returnsEmptyList() {
        val vm = HistoryViewModel(makeRepo(emptyList()))
        assertEquals(0, vm.runs.size)
    }
}
