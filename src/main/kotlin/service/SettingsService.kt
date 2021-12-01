package service

import NeuralLinkPluginSettings
import NeuralLinkState

@OptIn(ExperimentalJsExport::class)
@JsExport
class SettingsService(val state: NeuralLinkState) {
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
    fun loadFromJson(json: Any?) : NeuralLinkPluginSettings {
        // TODO: implement exmaple of versioned settings
        if (json == null) {
            state.settings = NeuralLinkPluginSettings.default()
        } else {
            // TODO ClassCastException here if there are no settings available? Maybe "result as String"?
            val loadedSettings = NeuralLinkPluginSettings.fromJson(json as String)
            console.log("loadedSettings: ", loadedSettings)
            // TODO Replace with a version check
            // Right now if fromJson fails the default settings will be used
            if (loadedSettings.taskRemoveRegex != "") {
                console.log("Returning loaded settings")
                state.settings = loadedSettings
            }
        }

        return state.settings
    }

    fun toJson() : String {
        val json = JSON.stringify(state.settings)
        console.log("saveSettings: ", json)
        return json
    }
}
