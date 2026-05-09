package com.runner.ui.tracking

import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class LocationViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val viewModel = LocationViewModel()

    @Test
    fun locationLiveData_initialValue_isNull() {
        assertNull(viewModel.locationLiveData.value)
    }

    @Test
    fun locationLiveData_whenValuePosted_observerReceivesValue() {
        val location = Location("test").apply {
            latitude = 10.0
            longitude = 20.0
            speed = 5.0f
            time = 1000L
        }

        var received: Location? = null
        viewModel.locationLiveData.observeForever { received = it }

        viewModel.locationLiveData.value = location

        assertEquals(10.0, received?.latitude)
        assertEquals(20.0, received?.longitude)
        assertEquals(5.0f, received?.speed)
        assertEquals(1000L, received?.time)
    }
}
