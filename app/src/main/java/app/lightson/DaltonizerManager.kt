package app.lightson

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Controls Android's colour-correction (daltonizer) filter through
 * Settings.Secure. Writing these keys requires WRITE_SECURE_SETTINGS,
 * which can only be granted over adb:
 *
 *   adb shell pm grant app.lightson android.permission.WRITE_SECURE_SETTINGS
 */
object DaltonizerManager {

    private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val DALTONIZER_MODE = "accessibility_display_daltonizer"

    /** Daltonizer mode 0 = simulate monochromacy, i.e. full grayscale. */
    private const val MODE_GRAYSCALE = 0

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun isFilterEnabled(context: Context): Boolean =
        Settings.Secure.getInt(context.contentResolver, DALTONIZER_ENABLED, 0) == 1

    /**
     * Enables or disables the grayscale filter.
     * Returns true when the setting was actually written.
     */
    fun setFilterEnabled(context: Context, enabled: Boolean): Boolean {
        if (!hasPermission(context)) return false
        return try {
            val resolver = context.contentResolver
            if (enabled) {
                Settings.Secure.putInt(resolver, DALTONIZER_MODE, MODE_GRAYSCALE)
            }
            Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, if (enabled) 1 else 0)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
