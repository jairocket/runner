package com.runner.ui.tracking

import android.location.Location
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel : ViewModel() {

    val locationLiveData = MutableLiveData<Location>()

    private val _locationHistory = mutableListOf<Location>()
    val locationHistory: List<Location> get() = _locationHistory

    private val _isTracking = MutableLiveData(false)
    val isTracking: LiveData<Boolean> = _isTracking

    private val _elapsedSeconds = MutableLiveData(0L)
    val elapsedSeconds: LiveData<Long> = _elapsedSeconds

    private val _distanceKm = MutableLiveData(0.0)
    val distanceKm: LiveData<Double> = _distanceKm

    private val _paceSecPerKm = MutableLiveData<Double?>(null)
    val paceSecPerKm: LiveData<Double?> = _paceSecPerKm

    private val _speedKmh = MutableLiveData<Float?>(null)
    val speedKmh: LiveData<Float?> = _speedKmh

    private val _trajectorySaved = MutableLiveData<Unit>()
    val trajectorySaved: LiveData<Unit> = _trajectorySaved

    private var lastLocation: Location? = null
    private var trackingStartMs = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            _elapsedSeconds.value = (SystemClock.elapsedRealtime() - trackingStartMs) / 1000
            handler.postDelayed(this, 1000)
        }
    }

    fun startTracking() {
        if (_isTracking.value == true) return
        _locationHistory.clear()
        _distanceKm.value = 0.0
        _paceSecPerKm.value = null
        _speedKmh.value = null
        _elapsedSeconds.value = 0L
        lastLocation = null
        trackingStartMs = SystemClock.elapsedRealtime()
        _isTracking.value = true
        handler.post(timerRunnable)
    }

    fun stopTracking() {
        if (_isTracking.value != true) return
        _isTracking.value = false
        handler.removeCallbacks(timerRunnable)
        lastLocation = null
        _speedKmh.value = null
    }

    fun resumeTracking() {
        if (_isTracking.value == true) return
        val elapsed = _elapsedSeconds.value ?: 0L
        if (elapsed == 0L) return
        trackingStartMs = SystemClock.elapsedRealtime() - elapsed * 1000
        lastLocation = null
        _isTracking.value = true
        handler.post(timerRunnable)
    }

    fun resetTimer() {
        if (_isTracking.value == true) return
        _elapsedSeconds.value = 0L
        _distanceKm.value = 0.0
        _paceSecPerKm.value = null
        _speedKmh.value = null
        _locationHistory.clear()
        _trajectorySaved.value = Unit
    }

    fun updateLocation(location: Location) {
        if (_isTracking.value == true) {
            if (location.hasAccuracy() && location.accuracy > 20f) return
            _locationHistory.add(location)
            val prev = lastLocation
            if (prev == null) {
                lastLocation = location
            } else {
                val deltaM = prev.distanceTo(location)
                val timeDeltaS = (location.time - prev.time) / 1000.0
                val isRealMovement = if (timeDeltaS > 0) deltaM / timeDeltaS >= 0.5 else deltaM > 0f
                if (isRealMovement) {
                    _speedKmh.value = if (timeDeltaS > 0) (deltaM / timeDeltaS * 3.6).toFloat() else null
                    val newDist = (_distanceKm.value ?: 0.0) + deltaM / 1000.0
                    _distanceKm.value = newDist
                    val secs = _elapsedSeconds.value ?: 0L
                    if (newDist >= 0.01 && secs > 0) {
                        _paceSecPerKm.value = secs.toDouble() / newDist
                    }
                    lastLocation = location
                } else {
                    _speedKmh.value = null
                }
            }
        }
        locationLiveData.value = location
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacks(timerRunnable)
    }
}
