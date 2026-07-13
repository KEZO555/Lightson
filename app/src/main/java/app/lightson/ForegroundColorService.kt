package app.lightson

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

/**
 * Watches window changes and toggles the grayscale filter automatically:
 * entering a "colour app" switches the filter off, leaving it switches the
 * filter back on. Mirrors Luma's colour filter toggler behaviour.
 *
 * Also listens for volume-key gestures (configurable in the app) so the
 * filter can be flipped manually from the hardware keys.
 */
class ForegroundColorService : AccessibilityService() {

    private var volumeUpHeld = false
    private var volumeDownHeld = false
    private var lastVolumeUpPress = 0L
    private var lastVolumeDownPress = 0L
    private var lastToggle = 0L

    private var foregroundPackage: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var cameraLongPressFired = false
    private val cameraLongPressRunnable = Runnable {
        cameraLongPressFired = true
        toggleFilter()
    }

    /** Packages that handle the camera intent — the shutter key is theirs. */
    private val cameraPackages: Set<String> by lazy {
        packageManager
            .queryIntentActivities(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), 0)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (shouldIgnore(pkg)) return
        foregroundPackage = pkg

        val prefs = Prefs(this)
        if (pkg in prefs.colorApps) {
            if (DaltonizerManager.isFilterEnabled(this) &&
                DaltonizerManager.setFilterEnabled(this, false)
            ) {
                prefs.weDisabledFilter = true
            }
        } else if (prefs.weDisabledFilter) {
            if (DaltonizerManager.setFilterEnabled(this, true)) {
                prefs.weDisabledFilter = false
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keymap = Prefs(this).keymap
        if (keymap == Prefs.KEYMAP_NONE) return false
        // Keymaps only apply inside apps — on the home screen (LightOS) the
        // keys keep their stock behaviour. Unknown foreground (e.g. right
        // after the service restarts) counts as home to stay hands-off.
        if (isHomeForeground()) return false
        val code = event.keyCode

        if (keymap == Prefs.KEYMAP_CAMERA_LONG_PRESS) {
            if (code != KeyEvent.KEYCODE_CAMERA) return false
            return onCameraKey(event)
        }
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) return false
                val now = event.eventTime
                if (code == KeyEvent.KEYCODE_VOLUME_UP) volumeUpHeld = true else volumeDownHeld = true
                when (keymap) {
                    Prefs.KEYMAP_VOLUME_CHORD ->
                        if (volumeUpHeld && volumeDownHeld) {
                            toggleFilter()
                            // Swallow the completing key so it doesn't also change the volume.
                            return true
                        }
                    Prefs.KEYMAP_DOUBLE_VOLUME_UP ->
                        if (code == KeyEvent.KEYCODE_VOLUME_UP) {
                            if (now - lastVolumeUpPress < DOUBLE_PRESS_WINDOW_MS) toggleFilter()
                            lastVolumeUpPress = now
                        }
                    Prefs.KEYMAP_DOUBLE_VOLUME_DOWN ->
                        if (code == KeyEvent.KEYCODE_VOLUME_DOWN) {
                            if (now - lastVolumeDownPress < DOUBLE_PRESS_WINDOW_MS) toggleFilter()
                            lastVolumeDownPress = now
                        }
                }
            }
            KeyEvent.ACTION_UP -> {
                if (code == KeyEvent.KEYCODE_VOLUME_UP) volumeUpHeld = false else volumeDownHeld = false
            }
        }
        return false
    }

    /**
     * Long-press toggles the filter; a short press keeps its usual meaning
     * by launching the camera ourselves, since a consumed key can't be
     * re-injected. While a camera app is in the foreground the key is left
     * alone entirely so the shutter keeps working.
     */
    private fun onCameraKey(event: KeyEvent): Boolean {
        if (foregroundPackage in cameraPackages) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    cameraLongPressFired = false
                    handler.postDelayed(cameraLongPressRunnable, LONG_PRESS_MS)
                }
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(cameraLongPressRunnable)
                if (!cameraLongPressFired) launchCamera()
                cameraLongPressFired = false
            }
        }
        return true
    }

    private fun isHomeForeground(): Boolean {
        val fg = foregroundPackage ?: return true
        val home = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
        return fg == home
    }

    private fun launchCamera() {
        try {
            startActivity(
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            // No camera app — nothing sensible to do with a short press.
        }
    }

    private fun toggleFilter() {
        val now = System.currentTimeMillis()
        if (now - lastToggle < TOGGLE_DEBOUNCE_MS) return
        lastToggle = now

        val enable = !DaltonizerManager.isFilterEnabled(this)
        if (DaltonizerManager.setFilterEnabled(this, enable)) {
            // Turning colour on is temporary: marking weDisabledFilter makes
            // the window watcher restore grayscale as soon as the user
            // leaves the current app.
            Prefs(this).weDisabledFilter = !enable
            Toast.makeText(
                this,
                if (enable) R.string.toast_filter_on else R.string.toast_filter_off,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() = Unit

    /**
     * Transient windows (system UI, keyboards) fire window-state events on
     * top of the real foreground app; reacting to them would flicker the
     * filter mid-use.
     */
    private fun shouldIgnore(pkg: String): Boolean {
        // Our own windows (main screen, toggle shortcut) must not count as
        // "left the app" — that would instantly undo a manual colour toggle.
        if (pkg == packageName) return true
        if (pkg == "com.android.systemui") return true
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == pkg }
    }

    companion object {
        private const val DOUBLE_PRESS_WINDOW_MS = 400L
        private const val TOGGLE_DEBOUNCE_MS = 500L
        private const val LONG_PRESS_MS = 500L
    }
}
