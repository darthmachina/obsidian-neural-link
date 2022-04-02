package service

import NeuralLinkPluginSettings
import Plugin
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.TaskModel
import org.reduxkotlin.Store
import store.UpdateSettings

@OptIn(ExperimentalJsExport::class)
@JsExport
class SettingsService(private val store: Store<TaskModel>, private val plugin: Plugin) {
    /**
     * Processes the results of a `loadData()` call.
     *
     * If there are no settings saved this will be the default settings. If there are settings but at a previous
     * version then the settings will be updated to the latest version by applying the default values for any new
     * settings.
     *
     * Settings are saved to the State but are also returned here for further processing if needed.
     *
     * @return A fully populated `NeuralLinkPluginSettings` object at the current version.
     */
    fun loadFromJson(json: Any?) {
        console.log("loadFromJson()")
        // TODO implement example of versioned settings
        if (json == null) {
            val newSettings = NeuralLinkPluginSettings.default()
            store.dispatch(UpdateSettings(plugin, this, newSettings.taskRemoveRegex, newSettings.columnTags))
        } else {
            val jsonSettings = Json.decodeFromString<NeuralLinkPluginSettings>(json as String)
            console.log(" - jsonSettings", jsonSettings)
            val loadedSettings = jsonSettings
            console.log(" - loadedSettings: ", loadedSettings)
            store.dispatch(UpdateSettings(plugin, this, loadedSettings.taskRemoveRegex, loadedSettings.columnTags))
        }
    }

    fun toJson(settings: NeuralLinkPluginSettings): String {
        val json = Json.encodeToString(settings)
        console.log("saveSettings: ", json)
        return json
    }
}
