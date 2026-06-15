package com.example.ecozaschitnik.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object AddressGeocoder {

    suspend fun resolve(context: Context, lat: Double, lon: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoderFromLocationApi33(geocoder, lat, lon)
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)
                }
                formatAddress(addresses?.firstOrNull())
            } catch (_: Exception) {
                null
            }
        }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun geocoderFromLocationApi33(
        geocoder: Geocoder,
        lat: Double,
        lon: Double,
    ) = suspendCancellableCoroutine { continuation ->
        geocoder.getFromLocation(lat, lon, 1) { addresses ->
            if (continuation.isActive) {
                continuation.resume(addresses)
            }
        }
    }

    private fun formatAddress(addr: android.location.Address?): String? {
        if (addr == null) return null

        val street = addr.thoroughfare
        val feature = addr.featureName?.takeIf { it != street }
        val city = addr.locality ?: addr.subAdminArea

        val nice = listOfNotNull(street, feature, city).joinToString(", ")
        return nice.ifEmpty { addr.getAddressLine(0) }
    }
}
