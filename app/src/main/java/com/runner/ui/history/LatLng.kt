package com.runner.ui.history

import android.os.Parcel
import android.os.Parcelable

data class LatLng(val lat: Double, val lon: Double) : Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(lat)
        parcel.writeDouble(lon)
    }

    companion object CREATOR : Parcelable.Creator<LatLng> {
        override fun createFromParcel(parcel: Parcel) = LatLng(parcel.readDouble(), parcel.readDouble())
        override fun newArray(size: Int): Array<LatLng?> = arrayOfNulls(size)
    }
}
