import kotlinx.html.dom.append
import kotlinx.html.js.h2
import org.w3c.dom.HTMLElement
import service.SettingsService

@OptIn(ExperimentalJsExport::class)
@JsExport
class NeuralLinkPluginSettingsTab(
    override var app: App,
    var plugin: NeuralLinkPlugin,
    private val settingsService: SettingsService,
    private val state: NeuralLinkState) : PluginSettingTab(app, plugin)
{
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
                    .setValue(state.settings.taskRemoveRegex)
                    .onChange { value ->
                        console.log("Regex: $value")
                        state.settings.taskRemoveRegex = value
                        plugin.saveData(settingsService.toJson())
                    }
            }
    }
}
