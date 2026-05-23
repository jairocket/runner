package com.runner.ui.history

import android.os.Parcel
import android.os.Parcelable

data class Position(val lat: Double, val lon: Double) : Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(lat)
        parcel.writeDouble(lon)
    }

    companion object CREATOR : Parcelable.Creator<Position> {
        override fun createFromParcel(parcel: Parcel) = Position(parcel.readDouble(), parcel.readDouble())
        override fun newArray(size: Int): Array<Position?> = arrayOfNulls(size)
    }
}
