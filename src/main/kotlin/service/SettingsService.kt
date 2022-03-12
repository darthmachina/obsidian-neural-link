package service

import NeuralLinkPluginSettings
import model.TaskModel
import org.reduxkotlin.Store
import store.UpdateSettings

@OptIn(ExperimentalJsExport::class)
@JsExport
class SettingsService(private val state: Store<TaskModel>) {
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
        // TODO implement example of versioned settings
        if (json == null) {
            val newSettings = NeuralLinkPluginSettings.default()
            state.dispatch(UpdateSettings(newSettings))
        } else {
            val loadedSettings = JSON.parse<NeuralLinkPluginSettings>(json as String)
            console.log("loadedSettings: ", loadedSettings)
            state.dispatch(UpdateSettings(loadedSettings))
        }
    }

    fun toJson(): String {
        val json = JSON.stringify(state.state.settings)
        console.log("saveSettings: ", json)
        return json
    }
}
