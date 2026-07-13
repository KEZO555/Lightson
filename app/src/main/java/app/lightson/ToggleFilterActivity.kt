package app.lightson

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible activity that flips the grayscale filter and exits immediately.
 * Exported so launcher gestures, app shortcuts and third-party key-mapper
 * apps (Key Mapper, Button Mapper, ...) can bind a key to it:
 *
 *   adb shell am start -n app.lightson/.ToggleFilterActivity
 */
class ToggleFilterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enable = !DaltonizerManager.isFilterEnabled(this)
        if (DaltonizerManager.setFilterEnabled(this, enable)) {
            if (!enable) Prefs(this).weDisabledFilter = false
            Toast.makeText(
                this,
                if (enable) R.string.toast_filter_on else R.string.toast_filter_off,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
