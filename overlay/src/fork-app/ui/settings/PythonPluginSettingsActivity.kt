package desu.inugram.ui.settings

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.exteragram.messenger.plugins.Plugin
import com.exteragram.messenger.plugins.PluginsController
import com.exteragram.messenger.plugins.models.CustomSetting
import com.exteragram.messenger.plugins.models.DividerSetting
import com.exteragram.messenger.plugins.models.EditTextSetting
import com.exteragram.messenger.plugins.models.HeaderSetting
import com.exteragram.messenger.plugins.models.InputSetting
import com.exteragram.messenger.plugins.models.SelectorSetting
import com.exteragram.messenger.plugins.models.SettingItem
import com.exteragram.messenger.plugins.models.SwitchSetting
import com.exteragram.messenger.plugins.models.TextSetting
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import java.util.IdentityHashMap

open class PythonPluginSettingsActivity(
    private val plugin: Plugin,
    private var targetSetting: String? = null,
    private val initialSettings: List<SettingItem> = emptyList(),
    private val onBackPressed: Runnable? = null,
    private val pageTitle: String? = null,
) : SettingsPageActivity(), NotificationCenter.NotificationCenterDelegate {
    private val controller get() = PluginsController.getInstance()
    private var rows = emptyList<SettingItem>()
    private val rowsByItem = IdentityHashMap<UItem, SettingItem>()
    private val ids = HashMap<SettingItem, Int>()
    private var resetItem: View? = null

    override fun getTitle(): CharSequence = pageTitle ?: plugin.getName()

    override fun onFragmentCreate(): Boolean {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pluginSettingsRegistered)
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pluginSettingsUnregistered)
        reloadRows()
        return super.onFragmentCreate()
    }

    override fun onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pluginSettingsRegistered)
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pluginSettingsUnregistered)
        onBackPressed?.run()
        super.onFragmentDestroy()
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        if (id == NotificationCenter.pluginSettingsRegistered || id == NotificationCenter.pluginSettingsUnregistered) {
            val targetId = args.firstOrNull() as? String
            if (targetId == null || targetId == plugin.getId()) {
                reloadRows()
            }
        }
    }

    override fun createView(context: Context): View {
        return super.createView(context).also {
            if (initialSettings.isEmpty()) {
                resetItem = simpleActionBar.createMenu().addItem(0, R.drawable.msg_reset).also { item ->
                    item.contentDescription = LocaleController.getString(R.string.PluginsResetSettings)
                    item.setOnClickListener { reset() }
                }
                updateResetItem()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reloadRows()
    }

    private fun updateResetItem() {
        resetItem?.visibility = if (controller.hasPluginSettingsPreferences(plugin.getId())) View.VISIBLE else View.GONE
    }

    fun reloadRows() {
        rows = initialSettings.ifEmpty {
            controller.getPluginSettingsList(plugin.getId()).orEmpty()
        }
        updateResetItem()
        listView?.adapter?.update(true)
    }

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        rowsByItem.clear()
        val installed = controller.plugins[plugin.getId()]
        if (installed == null) {
            items.add(UItem.asShadow(LocaleController.getString(R.string.PluginPermissionsGone)))
            return
        }
        installed.getError()?.localizedMessage?.takeIf { it.isNotBlank() }?.let { items.add(UItem.asShadow(it)) }
        if (!installed.isEnabled()) items.add(UItem.asShadow(LocaleController.getString(R.string.PluginsNotLoaded)))
        for (row in rows) {
            val id = ids.getOrPut(row) { ROW_BASE + ids.size }
            val item = rowItem(row, id) ?: continue
            rowsByItem[item] = row
            items.add(item)
        }
        if (rows.isEmpty() && installed.isEnabled()) {
            items.add(UItem.asShadow(LocaleController.getString(R.string.PluginsNoSettings)))
        }
        items.add(UItem.asShadow(null))
    }

    private fun rowItem(row: SettingItem, id: Int): UItem? = when (row) {
        is HeaderSetting -> UItem.asHeader(row.text)
        is DividerSetting -> UItem.asShadow(row.text?.takeIf { it.isNotBlank() })
        is SwitchSetting -> {
            val checked = controller.getPluginSettingBoolean(plugin.getId(), row.key, row.defaultValue)
            val subtext = row.subtext?.takeIf { it.isNotBlank() }
            if (subtext == null) {
                UItem.asCheck(id, row.text).also { it.checked = checked }
            } else {
                UItem.asButtonCheck(id, row.text, subtext).also { it.checked = checked }
            }
        }
        is SelectorSetting -> {
            val idx = controller.getPluginSettingInt(plugin.getId(), row.key, row.defaultValue)
            val value = row.items.getOrNull(idx).orEmpty()
            UItem.asButton(id, icon(row), row.text, value)
        }
        is InputSetting -> {
            val value = controller.getPluginSettingString(plugin.getId(), row.key, row.defaultValue).orEmpty()
            UItem.asButton(id, icon(row), row.text, value)
        }
        is EditTextSetting -> {
            val value = controller.getPluginSettingString(plugin.getId(), row.key, row.defaultValue).orEmpty()
            UItem.asButton(id, icon(row), row.hint ?: row.key, value)
        }
        is TextSetting -> {
            UItem.asButton(id, icon(row), row.text, null as String?).also {
                it.red = row.red
                it.accent = row.accent
            }
        }
        is CustomSetting -> null
        else -> null
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        val row = rowsByItem[item] ?: return
        when (row) {
            is SwitchSetting -> {
                val next = !controller.getPluginSettingBoolean(plugin.getId(), row.key, row.defaultValue)
                controller.setPluginSetting(plugin.getId(), row.key, next)
                try { row.onChangeCallback?.call(next) } catch (t: Throwable) { FileLog.e(t) }
                reloadRows()
            }
            is SelectorSetting -> {
                val current = controller.getPluginSettingInt(plugin.getId(), row.key, row.defaultValue)
                RadioItemOptions.show(this, view, row.items.toList(), current) { chosen ->
                    controller.setPluginSetting(plugin.getId(), row.key, chosen)
                    try { row.onChangeCallback?.call(chosen) } catch (t: Throwable) { FileLog.e(t) }
                    reloadRows()
                }
            }
            is InputSetting -> promptInput(row.text, row.key ?: "", row.defaultValue, false, 0, row.onChangeCallback)
            is EditTextSetting -> promptInput(row.hint ?: row.key, row.key ?: "", row.defaultValue, row.multiline, row.maxLength, row.onChangeCallback)
            is TextSetting -> {
                if (row.createSubFragmentCallback != null) {
                    try {
                        val sub = row.createSubFragmentCallback?.call() as? List<*> ?: emptyList<Any>()
                        val items = sub.filterIsInstance<SettingItem>()
                        presentFragment(PythonPluginSettingsActivity(plugin, null, items, null, row.text))
                    } catch (t: Throwable) {
                        FileLog.e(t)
                    }
                } else {
                    try { row.onClickCallback?.call(view) } catch (t: Throwable) { FileLog.e(t) }
                }
            }
        }
    }

    private fun promptInput(title: String?, key: String, defaultValue: String?, multiline: Boolean, maxLength: Int, onChange: com.chaquo.python.PyObject?) {
        val ctx = context ?: return
        val current = controller.getPluginSettingString(plugin.getId(), key, defaultValue).orEmpty()
        val edit = EditTextBoldCursor(ctx).apply {
            setTextSize(1, 16f)
            setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourceProvider))
            setHintTextColor(Theme.getColor(Theme.key_dialogTextHint, resourceProvider))
            setText(current)
            setSelection(current.length)
            if (multiline) {
                isSingleLine = false
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                gravity = Gravity.TOP or Gravity.START
            } else {
                isSingleLine = true
                inputType = InputType.TYPE_CLASS_TEXT
            }
            if (maxLength > 0) filters = arrayOf(InputFilter.LengthFilter(maxLength))
        }
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(FrameLayout(ctx).apply {
                setPadding(AndroidUtilities.dp(24f), AndroidUtilities.dp(8f), AndroidUtilities.dp(24f), AndroidUtilities.dp(8f))
                addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat()))
            })
        }
        showDialog(AlertDialog.Builder(ctx, resourceProvider)
            .setTitle(title ?: key)
            .setView(box)
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .setPositiveButton(LocaleController.getString(R.string.Save)) { _, _ ->
                val next = edit.text.toString()
                controller.setPluginSetting(plugin.getId(), key, next)
                try { onChange?.call(next) } catch (t: Throwable) { FileLog.e(t) }
                reloadRows()
            }.create())
    }

    private fun reset() {
        val ctx = context ?: return
        showDialog(AlertDialog.Builder(ctx, resourceProvider)
            .setTitle(LocaleController.getString(R.string.PluginsResetSettings))
            .setMessage(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.PluginsResetSettingsInfo, plugin.getName())))
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .setPositiveButton(LocaleController.getString(R.string.Reset)) { _, _ ->
                controller.clearPluginSettingsPreferences(plugin.getId(), false)
                reloadRows()
            }.create())
    }

    private fun icon(row: SettingItem): Int {
        val name = row.icon?.takeIf { it.isNotBlank() } ?: return 0
        val ctx = context ?: return 0
        return ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
    }

    companion object {
        private const val ROW_BASE = 100
    }
}
