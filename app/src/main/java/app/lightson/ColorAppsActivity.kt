package app.lightson

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Picker for the apps that should run in full colour. Selections take
 * effect immediately; the accessibility service reads them on every
 * window change.
 */
class ColorAppsActivity : AppCompatActivity() {

    private data class AppEntry(val label: String, val packageName: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_apps)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val list = findViewById<RecyclerView>(R.id.app_list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = AppAdapter(loadLaunchableApps(), Prefs(this))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadLaunchableApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .map { AppEntry(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private class AppAdapter(
        private val apps: List<AppEntry>,
        private val prefs: Prefs,
    ) : RecyclerView.Adapter<AppAdapter.Holder>() {

        class Holder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val checkbox: CheckBox = itemView.findViewById(R.id.app_checkbox)
            val label: TextView = itemView.findViewById(R.id.app_label)
            val pkg: TextView = itemView.findViewById(R.id.app_package)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return Holder(view)
        }

        override fun getItemCount() = apps.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val app = apps[position]
            holder.label.text = app.label
            holder.pkg.text = app.packageName
            holder.checkbox.setOnCheckedChangeListener(null)
            holder.checkbox.isChecked = app.packageName in prefs.colorApps
            holder.checkbox.setOnCheckedChangeListener { _, checked ->
                if (checked) prefs.addColorApp(app.packageName)
                else prefs.removeColorApp(app.packageName)
            }
            holder.itemView.setOnClickListener { holder.checkbox.toggle() }
        }
    }
}
