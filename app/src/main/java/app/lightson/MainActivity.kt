package app.lightson

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var filterSwitch: MaterialSwitch
    private lateinit var permissionCard: MaterialCardView
    private lateinit var serviceStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filterSwitch = findViewById(R.id.filter_switch)
        permissionCard = findViewById(R.id.permission_card)
        serviceStatus = findViewById(R.id.service_status)

        filterSwitch.setOnCheckedChangeListener { _, checked ->
            if (DaltonizerManager.isFilterEnabled(this) == checked) return@setOnCheckedChangeListener
            if (DaltonizerManager.setFilterEnabled(this, checked)) {
                if (!checked) Prefs(this).weDisabledFilter = false
            } else {
                // Write failed (permission missing) — snap the switch back.
                filterSwitch.isChecked = !checked
            }
        }

        findViewById<View>(R.id.color_apps_row).setOnClickListener {
            startActivity(Intent(this, ColorAppsActivity::class.java))
        }

        findViewById<View>(R.id.accessibility_row).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPermission = DaltonizerManager.hasPermission(this)
        permissionCard.visibility = if (hasPermission) View.GONE else View.VISIBLE
        filterSwitch.isEnabled = hasPermission
        filterSwitch.isChecked = DaltonizerManager.isFilterEnabled(this)
        serviceStatus.setText(
            if (isAccessibilityServiceEnabled()) R.string.service_on else R.string.service_off
        )
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
