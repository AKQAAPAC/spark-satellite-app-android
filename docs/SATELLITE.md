# The satellite (connection-aware) feature

This document explains how **satellite connectivity** works in this app: what it means, how to see it and test it during development, and how it is implemented in terms of **connectivity** and **location** only.

## What “satellite” means in this app

- **Good** — Wi‑Fi, Ethernet, or cellular (4G/5G). The app shows **"Status: Good data"**; full map and forecast, rain map loaded.
- **Low** — Satellite transport only (API 31+). The app shows **"Status: Low data"**; reduced experience (e.g. no rain map).
- **None** — No network. The app shows **"Status: No data"**; minimal or no data, no rain map.

The status bar shows a single line: **Status: Good data** / **Status: Low data** / **Status: No data**, aligned with map and forecast visibility.

## How to see it in the demo (development and testing)

1. **On a real device** — Wi‑Fi and cellular (4G/5G) → Good. Only a link that reports **TRANSPORT_SATELLITE** (API 31+) shows Low; use the override in step 3 if you don’t have satellite hardware.
2. **On an emulator** — Emulators typically report Wi‑Fi or cellular, so you’ll see Good unless you use the override.
3. **Testing override** — To force a status: `adb shell am start -n com.akqa.sparksatelliteweather/.MainActivity --es connectivity_override good` (use `good`, `low`, or `none`). Value is persisted until you change it or clear app data.
4. **Behaviour** — When connectivity is **Good**, the rain map (radar) is loaded; when **Low** or **None**, the rain map section shows a placeholder. So the app both shows the satellite state and acts on it.

**Location:** Weather and RainViewer radar use the **device location** from Fused Location Provider. There is no fallback location; if location is unavailable, the app shows a short explanation and **Open Settings** (see **`location_prompt_settings`** / **`open_settings`** in **`res/values/strings.xml`**).

## How it’s implemented (connectivity and location only)

### 1. Connection strength — `Connectivity.kt`

- **`Connectivity`** is an enum: `Good`, `Low`, `None`.
- **`ConnectivityManager.connectivityFlow(context)`** is a `Flow<Connectivity>` that registers a `NetworkCallback` and emits the current connectivity. If a testing override is set (intent extra `connectivity_override`), that value is used. Otherwise: **Good** = Wi‑Fi, Ethernet, or cellular (4G/5G). **Low** = satellite transport only (`NetworkCapabilities.TRANSPORT_SATELLITE`, API 31+). **None** = no active network.

So **TRANSPORT_SATELLITE** is what drives **Low** (and therefore **"Status: Low data"**) on real hardware; use the override for testing without satellite.

### Older vs newer Android versions (for app developers)

Use the same **Good / Low / None** product model on all versions. Gate APIs by SDK level so one codebase covers old and new devices.

| Android / API | What you can rely on | What to do in your app |
|---------------|----------------------|-------------------------|
| **API 26–30** (this app’s minSdk up to pre‑12) | No public satellite transport in normal app networking | Treat connectivity as **Good** (Wi‑Fi / Ethernet / cellular) or **None**. Do not expect real satellite Low. Use a **test override** in demos. |
| **API 31–34** | Satellite transport may appear on some OEM/extension builds; constant availability varies | Keep a version check before reading satellite transport (this app uses `Build.VERSION_CODES.S`). Prefer the numeric transport id `10` only if you must support older compile SDKs—prefer the SDK constant when `compileSdk` is high enough. |
| **API 35+ (Android 15)** | `TRANSPORT_SATELLITE` is the supported ConnectivityManager signal; constrained satellite opt‑in is documented | Opt in with `PROPERTY_SATELLITE_DATA_OPTIMIZED`. Detect Low with `TRANSPORT_SATELLITE` (and optionally `!NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED` per Google’s [constrained satellite guide](https://developer.android.com/develop/connectivity/satellite/constrained-networks)). Use a `NetworkRequest` that **removes** `NOT_BANDWIDTH_CONSTRAINED` if you need callbacks for constrained nets. |
| **API 36–37+** | Same connectivity model; NTN signal APIs appear on telephony (`NtnSignalStrength`, carrier‑roaming NTN listeners) | Optional: show signal quality **in addition to** Good/Low/None. Do **not** replace ConnectivityManager transport checks with NTN signal alone—signal APIs are carrier/telephony‑oriented and not the primary “adapt bandwidth” switch. |

**Practical pattern (matches this demo):**

1. Always classify Wi‑Fi / Ethernet / cellular as **Good** and no network as **None**.
2. On API levels where satellite transport is available, classify validated satellite as **Low** and gate heavy features (maps, video, large downloads).
3. Always ship the manifest opt‑in if you want HTTPS traffic on constrained satellite when that is the only network.
4. Keep an **adb / intent override** so QA can exercise Low without satellite hardware on any API level.

**This app today:** `minSdk 26`, `targetSdk 36`, `compileSdk 37`. Detection uses `TRANSPORT_SATELLITE` with an API 31+ guard; constrained capability detection is documented as an optional alignment with Google’s sample, not required for the demo’s Good/Low/None UX.

### 2. Observing connectivity — `WeatherViewModel.kt`

- The ViewModel receives a **`ConnectivityManager`** and subscribes to **`connectivityManager.connectivityFlow(context)`** in `init`, updating **`state.connectivity`** on each emission.
- When loading data, the ViewModel uses **`state.connectivity`**: the rain map is only fetched when **`Connectivity.Good`**. The **hourly** strip is driven by **`daily`**, **`hourly`**, and **`selectedDate`** only: it is shown in **`ContentView`** whenever there is daily forecast data, not hidden when connectivity is **None** (after a successful load, hours stay in sync with the selected day).

### 3. Status bar — `ui/ContentView.kt`

- **StatusBar** receives **`connectivity`** from **`state.connectivity`** and **`lastWeatherFetchAtMillis`** from state. Status text comes from **`strings.xml`** (**connectivity_good** / **connectivity_low** / **connectivity_none**). **Refresh** is underlined; below it, a **short + medium** locale date/time is shown after a successful weather load (hidden after API or location failure).

### 4. Location — `LocationHelper.kt`

- **Fused Location Provider** supplies the device location; **reverse geocoding** gives the place name. **Open-Meteo** and **RainViewer** use this location for weather and radar. **`getCurrentLocation(forceRefresh)`** is used so that a user-triggered Refresh uses a fresh location; initial load may use a cached fix.

## Summary

| Concept | In this app |
|--------|----------------|
| **What triggers Low data mode** | TRANSPORT_SATELLITE (API 31+ guard in code; public ConnectivityManager constant from API 35+) on the active network; or testing override `connectivity_override`. |
| **Where it’s computed** | `Connectivity.kt`: `currentConnectivity()` and `connectivityFlow()`. |
| **Where it’s stored** | `WeatherViewModel`: `state.connectivity` updated from the flow. |
| **Where it’s shown** | `ContentView.kt`: StatusBar shows `strings.xml` status lines + last successful fetch time. |
| **Where it’s used for behaviour** | ViewModel: rain map only when `Connectivity.Good`. Hourly UI whenever `daily` is non-empty (see `recalculateDisplayHours` / `withDisplayHoursRecalculated`). |
| **Location** | `LocationHelper.kt`: device location for weather and radar; no fallback. |
| **Traffic on satellite** | `AndroidManifest.xml`: `PROPERTY_SATELLITE_DATA_OPTIMIZED` meta-data (see **Building an app**). |

For the exact code, see **`Connectivity.kt`**, **`WeatherViewModel.kt`** (init and state), **`ui/ContentView.kt`** (StatusBar), and **`LocationHelper.kt`**.

---

## Building an app with satellite (connection-aware) behaviour

If you want to add similar behaviour to your own app:

1. **Observe connectivity** — Use `ConnectivityManager.registerDefaultNetworkCallback` (or a constrained-aware `NetworkRequest` + `registerBestMatchingNetworkCallback` on newer APIs) and `NetworkCapabilities`. Treat `TRANSPORT_SATELLITE` as a “low” or constrained state when available; Wi‑Fi, Ethernet, and cellular as “good”; no network as “none”. See **`Connectivity.kt`** and the **Older vs newer Android versions** section above.
2. **Opt in to constrained satellite data (so HTTPS works on satellite)** — Under `<application>` in **`AndroidManifest.xml`**, add:

   ```xml
   <meta-data
       android:name="android.telephony.PROPERTY_SATELLITE_DATA_OPTIMIZED"
       android:value="${applicationId}" />
   ```

   Without this, the system may not route your app’s traffic over satellite when that is the only network. [Develop for constrained satellite networks](https://developer.android.com/develop/connectivity/satellite/constrained-networks).

3. **Expose a single state** — Your UI should react to one value (e.g. Good / Low / None). The ViewModel subscribes to the connectivity flow and keeps that state.
4. **Gate heavy or optional features** — In this app, the rain map is only loaded when connectivity is Good. You can hide or downgrade bandwidth-heavy features when Low or None.
5. **Optional: minimal data when None** — When there is no network, fetch only essential data (e.g. current + daily, no hourly) to avoid failing requests.
6. **Testing** — Support an override (e.g. intent extra or shared prefs) so you can force Good / Low / None without real satellite hardware. See the `connectivity_override` handling in **`Connectivity.kt`**.

**iOS (if you also ship an iPhone app):** Apple uses **entitlements** for ultra-constrained networks, not this manifest flag. See the **SparkSatelliteWeather-iOS** **`docs/SATELLITE.md`** “Building an app” section and Apple’s [Configuring your app for ultra-constrained networks](https://developer.apple.com/documentation/BundleResources/Configuring-your-app-for-ultra-constrained-networks).
