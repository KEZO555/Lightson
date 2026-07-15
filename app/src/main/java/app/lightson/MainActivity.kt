package app.lightson

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var permissionCard: MaterialCardView
    private lateinit var serviceStatus: TextView
    private lateinit var keymapValue: TextView

    private val keymapModes = intArrayOf(
        Prefs.KEYMAP_NONE,
        Prefs.KEYMAP_CAMERA_LONG_PRESS,
        Prefs.KEYMAP_VOLUME_CHORD,
        Prefs.KEYMAP_DOUBLE_VOLUME_UP,
        Prefs.KEYMAP_DOUBLE_VOLUME_DOWN,
    )

    private val keymapLabels = intArrayOf(
        R.string.keymap_none,
        R.string.keymap_camera_long_press,
        R.string.keymap_volume_chord,
        R.string.keymap_double_volume_up,
        R.string.keymap_double_volume_down,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionCard = findViewById(R.id.permission_card)
        serviceStatus = findViewById(R.id.service_status)
        keymapValue = findViewById(R.id.keymap_value)

        val closeOnLock = findViewById<MaterialSwitch>(R.id.close_on_lock_switch)
        closeOnLock.isChecked = Prefs(this).closeAppsOnLock
        closeOnLock.setOnCheckedChangeListener { _, checked ->
            Prefs(this).closeAppsOnLock = checked
        }

        findViewById<View>(R.id.color_apps_row).setOnClickListener {
            startActivity(Intent(this, ColorAppsActivity::class.java))
        }

        findViewById<View>(R.id.keymap_row).setOnClickListener { showKeymapDialog() }

        findViewById<View>(R.id.accessibility_row).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun showKeymapDialog() {
        val prefs = Prefs(this)
        val checked = keymapModes.indexOf(prefs.keymap).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.keymap_dialog_title)
            .setSingleChoiceItems(keymapLabels.map { getString(it) }.toTypedArray(), checked) { dialog, which ->
                prefs.keymap = keymapModes[which]
                updateKeymapRow()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateKeymapRow() {
        val index = keymapModes.indexOf(Prefs(this).keymap).coerceAtLeast(0)
        val label = getString(keymapLabels[index])
        keymapValue.text =
            if (keymapModes[index] == Prefs.KEYMAP_NONE) label
            else "$label — ${getString(R.string.keymap_needs_service)}"
    }

    override fun onResume() {
        super.onResume()
        val hasPermission = DaltonizerManager.hasPermission(this)
        permissionCard.visibility = if (hasPermission) View.GONE else View.VISIBLE
        serviceStatus.setText(
            if (isAccessibilityServiceEnabled()) R.string.service_on else R.string.service_off
        )
        updateKeymapRow()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = "$packageName/${ForegroundColorService::class.java.name}"
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
    }
}
