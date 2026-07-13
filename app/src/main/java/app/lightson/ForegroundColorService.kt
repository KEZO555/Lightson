package app.lightson

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * Watches window changes and toggles the grayscale filter automatically:
 * entering a "colour app" switches the filter off, leaving it switches the
 * filter back on. Mirrors Luma's colour filter toggler behaviour.
 */
class ForegroundColorService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (shouldIgnore(pkg)) return

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

    override fun onInterrupt() = Unit

    /**
     * Transient windows (system UI, keyboards) fire window-state events on
     * top of the real foreground app; reacting to them would flicker the
     * filter mid-use.
     */
    private fun shouldIgnore(pkg: String): Boolean {
        if (pkg == "com.android.systemui") return true
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == pkg }
    }
}
