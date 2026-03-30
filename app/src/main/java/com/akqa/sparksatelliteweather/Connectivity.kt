package com.akqa.sparksatelliteweather

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Testing override: prefs name and key (values: "good", "low", "none"). Set via intent extra or adb. */
internal const val CONNECTIVITY_OVERRIDE_PREF = "spark_connectivity_override"

/**
 * Connection strength for the **default** (active) network only.
 *
 * We use [ConnectivityManager.registerDefaultNetworkCallback] and
 * [NetworkCapabilities].
 *
 * **Good** = Wi‑Fi, Ethernet, or cellular (4G/5G). Normal mobile data is treated as Good so the app
 * works as expected when the internet is available.
 * **Low** = satellite transport (API 31+) **and** validated internet on that network.
 * **None** = no network.
 *
 * For local testing you can override: launch with intent extra `connectivity_override` = `good` | `low` | `none`
 * (e.g. `adb shell am start -n com.akqa.sparksatelliteweather/.MainActivity --es connectivity_override good`).
 *
 */
/** Good / Low / None — status bar labels are in `res/values/strings.xml` (`connectivity_*`). */
enum class Connectivity {
    Good,
    Low,
    None;
}

/** Observe in ViewModel: connectivityFlow(context).onEach { ... }.launchIn(viewModelScope). */
fun ConnectivityManager.connectivityFlow(context: Context): Flow<Connectivity> = callbackFlow {
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            trySend(currentConnectivity(context))
        }
        override fun onLost(network: Network) {
            trySend(currentConnectivity(context))
        }
    }
    registerDefaultNetworkCallback(callback)
    trySend(currentConnectivity(context))
    awaitClose { unregisterNetworkCallback(callback) }
}

private fun ConnectivityManager.currentConnectivity(context: Context): Connectivity {
    // Testing override (e.g. adb --es connectivity_override good|low|none).
    context.getSharedPreferences(CONNECTIVITY_OVERRIDE_PREF, Context.MODE_PRIVATE)
        .getString(CONNECTIVITY_OVERRIDE_PREF, null)
        ?.let { when (it) {
            "good" -> return Connectivity.Good
            "low" -> return Connectivity.Low
            "none" -> return Connectivity.None
            else -> { }
        } }

    val network = activeNetwork ?: return Connectivity.None
    val caps = getNetworkCapabilities(network) ?: return Connectivity.None

    // Wi‑Fi or Ethernet → Good (so emulator/simulator on "Wi-Fi" shows Good).
    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
        return Connectivity.Good
    }

    // Satellite: only Low when the OS has validated internet; otherwise show None (no usable data).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        caps.hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE)) {
        return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            Connectivity.Low
        } else {
            Connectivity.None
        }
    }

    // Cellular (4G/5G): treat as Good so the app works normally when mobile data is available.
    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
        return Connectivity.Good
    }

    // Unknown transport (e.g. VPN or emulator default): treat as Good so simulator isn’t forced to Low.
    return Connectivity.Good
}
