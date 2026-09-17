# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - Toolchain upgrade

- Toolchain upgrade: AGP 9.4, Gradle 9.7.1, Kotlin 2.4, Compose BOM 2026.08, compileSdk 37 / targetSdk 36.
- Library upgrades: Coil 3, Retrofit 3, OkHttp 5, maps-compose 8.6, Play services maps/location.
- Docs: older vs newer Android guidance for satellite / constrained networking in `docs/SATELLITE.md`.
- Add Gradle wrapper scripts; enable Quail parallel sync property.
- Requires Android Studio Quail 4 (or newer). For older Studio / toolchain, use the [`v1.0.0`](https://github.com/AKQAAPAC/spark-satellite-app-android/releases/tag/v1.0.0) release.

## [1.0.0] - Initial release

- Connection-aware weather demo (status bar: Good / Low / No data; rain map gating).
- Location-based weather via Open-Meteo and Fused Location Provider; RainViewer radar map.
- Maintained by AKQA.
