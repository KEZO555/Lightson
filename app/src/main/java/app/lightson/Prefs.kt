package app.lightson

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lightson", Context.MODE_PRIVATE)

    /** Packages that should be shown in full colour. */
    var colorApps: Set<String>
        get() = prefs.getStringSet(KEY_COLOR_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_COLOR_APPS, value).apply()

    /**
     * True while the accessibility service has turned the filter off for a
     * colour app and still owes the system a re-enable. Persisted so the
     * filter is restored even if the process is killed in between.
     */
    var weDisabledFilter: Boolean
        get() = prefs.getBoolean(KEY_WE_DISABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_WE_DISABLED, value).apply()

    /** Hardware key gesture that toggles the filter, one of the KEYMAP_* values. */
    var keymap: Int
        get() = prefs.getInt(KEY_KEYMAP, KEYMAP_NONE)
        set(value) = prefs.edit().putInt(KEY_KEYMAP, value).apply()

    fun addColorApp(packageName: String) {
        colorApps = colorApps + packageName
    }

    fun removeColorApp(packageName: String) {
        colorApps = colorApps - packageName
    }

    companion object {
        private const val KEY_COLOR_APPS = "color_apps"
        private const val KEY_WE_DISABLED = "we_disabled_filter"
        private const val KEY_KEYMAP = "keymap"

        const val KEYMAP_NONE = 0
        const val KEYMAP_VOLUME_CHORD = 1
        const val KEYMAP_DOUBLE_VOLUME_UP = 2
        const val KEYMAP_DOUBLE_VOLUME_DOWN = 3
        const val KEYMAP_CAMERA_LONG_PRESS = 4
    }
}
