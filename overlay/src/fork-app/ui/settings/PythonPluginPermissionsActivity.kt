package desu.inugram.ui.settings

import android.view.View
import com.exteragram.messenger.plugins.PluginsController
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter

class PythonPluginPermissionsActivity(private val pluginId: String) : SettingsPageActivity() {
    private val controller get() = PluginsController.getInstance()

    override fun getTitle(): CharSequence = LocaleController.getString(R.string.PluginPermissions)

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {}

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        val plugin = controller.plugins[pluginId]
        if (plugin == null) {
            items.add(UItem.asShadow(LocaleController.getString(R.string.PluginPermissionsGone)))
            return
        }
        items.add(UItem.asHeader(plugin.getName()))
        plugin.getAuthor()?.takeIf { it.isNotBlank() }?.let {
            items.add(UItem.asButton(1, LocaleController.getString(R.string.PluginNoAuthor), it))
        }
        plugin.getVersion()?.takeIf { it.isNotBlank() }?.let {
            items.add(UItem.asButton(2, LocaleController.formatString(R.string.PluginsInstallVersion, it)))
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.PluginsUnsafeModeInfo)))
    }
}
