package com.runner.ui.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MockRunRepositoryTest {

    private val repo = MockRunRepository()

    @Test
    fun getAll_returnsSixRuns() {
        assertEquals(6, repo.getAll().size)
    }

    @Test
    fun getById_returnsCorrectRun() {
        val run = repo.getById("1")
        assertEquals("May 14, 2026", run?.date)
        assertEquals("6.2 km", run?.distanceKm)
    }

    @Test
    fun getById_unknownId_returnsNull() {
        assertNull(repo.getById("999"))
    }

    @Test
    fun getAll_firstRunHasPositions() {
        val run = repo.getAll().first()
        assert(run.positions.isNotEmpty())
    }
}
