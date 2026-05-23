package com.runner.ui.history

import android.os.Parcelable

data class LatLng(val lat: Double, val lon: Double) : Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeDouble(lat)
        parcel.writeDouble(lon)
    }

    companion object CREATOR : android.os.Parcelable.Creator<LatLng> {
        override fun createFromParcel(parcel: android.os.Parcel): LatLng {
            return LatLng(parcel.readDouble(), parcel.readDouble())
        }

        override fun newArray(size: Int): Array<LatLng?> = arrayOfNulls(size)
    }
}
