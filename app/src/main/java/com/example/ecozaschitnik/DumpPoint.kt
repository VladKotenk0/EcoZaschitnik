package com.example.ecozaschitnik

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DumpPoint(
    val id: String,
    val lat: Double,
    val lon: Double,
    val title: String = "Свалка",
    val description: String = "",
    val reportText: String = "",
    val coordinatesText: String = "",
    val photoUrl: String? = null,
    val hasPhoto: Boolean = false,
    val timestamp: Long? = null,
) : Parcelable {
    fun hasPhotoAttachment(): Boolean = hasPhoto || !photoUrl.isNullOrBlank()
}
