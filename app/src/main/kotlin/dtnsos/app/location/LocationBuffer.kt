package dtnsos.app.location

import android.content.Context
import android.content.SharedPreferences

data class CachedLocation(
    val latitude: Double,
    val longitude: Double,
    val timestampMs: Long,
    val source: String
)

/**
 * Buffer for caching the last known location when GPS is unavailable
 * or device is completely offline with no cellular towers.
 * Persists in SharedPreferences.
 */
class LocationBuffer(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dtnsos_location_buffer", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAT = "cached_latitude"
        private const val KEY_LON = "cached_longitude"
        private const val KEY_TS = "cached_timestamp"
        private const val KEY_SOURCE = "cached_source"

        // Default disaster zone preset if device has never had a GPS lock (Kolkata PRD default)
        const val DEFAULT_LAT = 22.5726
        const val DEFAULT_LON = 88.3639
        const val DEFAULT_SOURCE = "Default Offline Preset"
    }

    fun saveLocation(lat: Double, lon: Double, source: String = "GPS Lock") {
        prefs.edit()
            .putString(KEY_LAT, lat.toString())
            .putString(KEY_LON, lon.toString())
            .putLong(KEY_TS, System.currentTimeMillis())
            .putString(KEY_SOURCE, source)
            .apply()
    }

    fun getLastKnownLocation(): CachedLocation {
        val latStr = prefs.getString(KEY_LAT, null)
        val lonStr = prefs.getString(KEY_LON, null)
        val ts = prefs.getLong(KEY_TS, 0L)
        val source = prefs.getString(KEY_SOURCE, DEFAULT_SOURCE) ?: DEFAULT_SOURCE

        val lat = latStr?.toDoubleOrNull() ?: DEFAULT_LAT
        val lon = lonStr?.toDoubleOrNull() ?: DEFAULT_LON

        return CachedLocation(
            latitude = lat,
            longitude = lon,
            timestampMs = ts,
            source = source
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
