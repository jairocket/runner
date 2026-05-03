package com.example.runner.ui.tracking

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel: ViewModel()    {
    val locationLiveData = MutableLiveData<Location>()
}