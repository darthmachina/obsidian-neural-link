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

        containerEl.append.h2 { +"Neural Link Settings." }
        createTaskTextRemovalRegexSetting(containerEl)
    }

    private fun createTaskTextRemovalRegexSetting(containerEl: HTMLElement) : Setting {
        return Setting(containerEl)
            .setName("Task Text Removal Regex")
            .setDesc("Contents to remove from task on completion")
            .addText { text ->
                text.setPlaceholder("Regex")
                    .setValue(plugin.settings.taskRemoveRegex)
                    .onChange { value ->
                        console.log("Regex: $value")
                        plugin.settings.taskRemoveRegex = value
                        saveSettings()
                    }
            }
    }

    private fun saveSettings() {
        console.log("saveSettings: ", NeuralLinkPluginSettings.toJson(plugin.settings))
        plugin.saveData(NeuralLinkPluginSettings.toJson(plugin.settings))
    }
}