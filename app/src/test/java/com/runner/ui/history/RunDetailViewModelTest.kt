package com.runner.ui.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunDetailViewModelTest {

    private fun makeRepo(runs: List<RunActivity> = mockRuns): RunRepository =
        object : RunRepository {
            override fun getAll() = runs
            override fun getById(id: String) = runs.find { it.id == id }
        }

    @Test
    fun run_returnsCorrectRunForValidId() {
        val vm = RunDetailViewModel(makeRepo(), "1")
        assertEquals("May 14, 2026", vm.run?.date)
        assertEquals("6.2 km", vm.run?.distanceKm)
    }

    @Test
    fun run_returnsNullForUnknownId() {
        val vm = RunDetailViewModel(makeRepo(), "999")
        assertNull(vm.run)
    }

    @Test
    fun run_positionsArePresent() {
        val vm = RunDetailViewModel(makeRepo(), "1")
        assertTrue((vm.run?.positions?.size ?: 0) > 0)
    }
}
