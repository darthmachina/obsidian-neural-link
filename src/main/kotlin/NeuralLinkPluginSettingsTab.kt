import kotlinx.html.dom.append
import kotlinx.html.js.h2
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalJsExport::class)
@JsExport
class NeuralLinkPluginSettingsTab(override var app: App, var plugin: NeuralLinkPlugin) : PluginSettingTab(app, plugin) {
    override fun display() {
        while(containerEl.firstChild != null) {
            containerEl.lastChild?.let { containerEl.removeChild(it) }
        }

        containerEl.append.h2 { +"Settings for my awesome plugin." }
        createSampleSetting(containerEl)
    }

    private fun createSampleSetting(containerEl: HTMLElement) : Setting {
        return Setting(containerEl)
            .setName("Setting #1")
            .setDesc("It's a secret")
            .addText { text ->
                text.setPlaceholder("Enter your secret")
                    .setValue(plugin.settings.mySetting)
                    .onChange { value ->
                        console.log("Secret: $value")
                        plugin.settings.mySetting = value
                        saveSettings()
                    }
            }
    }

    private fun saveSettings() {
        console.log("saveSettings: ", NeuralLinkPluginSettings.toJson(plugin.settings))
        plugin.saveData(NeuralLinkPluginSettings.toJson(plugin.settings))
    }
}