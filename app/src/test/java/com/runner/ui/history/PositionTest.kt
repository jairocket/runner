package com.runner.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class PositionTest {

    @Test
    fun constructor_storesLatAndLon() {
        val point = Position(-25.4284, -49.2733)
        assertEquals(-25.4284, point.lat, 0.0001)
        assertEquals(-49.2733, point.lon, 0.0001)
    }

    @Test
    fun dataClass_equalityByValue() {
        val a = Position(-25.4284, -49.2733)
        val b = Position(-25.4284, -49.2733)
        assertEquals(a, b)
    }
}
