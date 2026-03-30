package com.akqa.sparksatelliteweather

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import java.util.Locale

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val placeName: String?
)

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.resolvePlaceName(lat: Double, lon: Double): String? = try {
    Geocoder(this, Locale.getDefault())
        .getFromLocation(lat, lon, 1)
        ?.firstOrNull()
        ?.let { addr ->
            val subLocality = addr.subLocality?.takeIf { it.isNotEmpty() }
            val locality = addr.locality?.takeIf { it.isNotEmpty() && it != subLocality }
            val adminArea = addr.adminArea?.takeIf { it.isNotEmpty() && it != locality }
            listOfNotNull(subLocality, locality, adminArea)
                .joinToString(", ")
                .ifEmpty { addr.locality ?: addr.subAdminArea ?: addr.adminArea }
        }
} catch (_: Exception) {
    null
}

/** Max wait for a single getCurrentLocation() call. Without this, the call can block indefinitely if GPS never resolves (e.g. no signal); then we fall back to lastLocation. */
private const val LOCATION_TIMEOUT_MS = 10_000L

suspend fun Context.getCurrentLocation(forceRefresh: Boolean = false): LocationResult? = withContext(kotlinx.coroutines.Dispatchers.IO) {
    if (!hasLocationPermission()) return@withContext null
    val fused = LocationServices.getFusedLocationProviderClient(this@getCurrentLocation)

    // 1) Try last known location first (fast). Skip when user tapped Refresh.
    if (!forceRefresh) {
        val last = suspendCancellableCoroutine<Location?> { cont ->
            fused.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
        if (last != null) {
            val name = resolvePlaceName(last.latitude, last.longitude)
            return@withContext LocationResult(last.latitude, last.longitude, name)
        }
    }

    // 2) Request a fresh fix with timeout. Outdoor/satellite use; HIGH_ACCURACY for clear-sky scenario.
    val cts = CancellationTokenSource()
    val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
        suspendCancellableCoroutine<Location?> { cont ->
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    }
    if (fresh != null) {
        val name = resolvePlaceName(fresh.latitude, fresh.longitude)
        return@withContext LocationResult(fresh.latitude, fresh.longitude, name)
    }

    // 3) Timeout or null: try lastLocation again in case the system updated it while we waited.
    delay(500)
    val lastAgain = suspendCancellableCoroutine<Location?> { cont ->
        fused.lastLocation
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }
    lastAgain?.let {
        val name = resolvePlaceName(it.latitude, it.longitude)
        LocationResult(it.latitude, it.longitude, name)
    } ?: return@withContext null
}
