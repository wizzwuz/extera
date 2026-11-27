package com.exteragram.messenger.plugins.ui

import com.exteragram.messenger.plugins.Plugin
import com.exteragram.messenger.plugins.PluginsController
import com.exteragram.messenger.plugins.models.SettingItem
import desu.inugram.ui.settings.PythonPluginSettingsActivity
import org.telegram.messenger.AndroidUtilities

class PluginSettingsActivity : PythonPluginSettingsActivity {

    @JvmOverloads
    constructor(
        plugin: Plugin,
        targetSettingName: String? = null,
    ) : super(plugin, targetSettingName, emptyList(), null)

    @JvmOverloads
    constructor(
        plugin: Plugin,
        title: String?,
        settingsList: List<SettingItem>?,
        createSubFragmentCallback: Any?,
        targetSettingName: String? = null,
    ) : super(plugin, title ?: targetSettingName, settingsList ?: emptyList(), createSubFragmentCallback as? Runnable)

    constructor(pluginId: String) : this(requirePlugin(pluginId))

    fun setSettingsLinkPrefix(prefix: String?): PluginSettingsActivity = this

    fun updateItems() {
        AndroidUtilities.runOnUIThread { reloadRows() }
    }

    companion object {
        private fun requirePlugin(pluginId: String): Plugin {
            return PluginsController.getInstance().plugins[pluginId]
                ?: throw IllegalArgumentException("Plugin is not installed: $pluginId")
        }
    }
}
