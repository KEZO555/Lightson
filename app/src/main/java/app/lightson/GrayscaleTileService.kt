package app.lightson

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile to flip the grayscale filter manually. */
class GrayscaleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val enable = !DaltonizerManager.isFilterEnabled(this)
        if (DaltonizerManager.setFilterEnabled(this, enable) && !enable) {
            // A manual switch to colour overrides any pending auto-restore.
            Prefs(this).weDisabledFilter = false
        }
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        tile.state = when {
            !DaltonizerManager.hasPermission(this) -> Tile.STATE_UNAVAILABLE
            DaltonizerManager.isFilterEnabled(this) -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.tile_label)
        tile.updateTile()
    }
}
