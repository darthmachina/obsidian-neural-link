import kotlinx.html.dom.append
import kotlinx.html.js.h2
import model.StatusTag
import model.TaskModel
import org.reduxkotlin.Store
import org.w3c.dom.HTMLElement
import service.SettingsService

@OptIn(ExperimentalJsExport::class)
@JsExport
class NeuralLinkPluginSettingsTab(
    override var app: App,
    private var plugin: NeuralLinkPlugin,
    private val settingsService: SettingsService,
    private val store: Store<TaskModel>
) : PluginSettingTab(app, plugin) {
    override fun display() {
        while (containerEl.firstChild != null) {
            containerEl.lastChild?.let { containerEl.removeChild(it) }
        }

        containerEl.append.h2 { +"Neural Link Settings." }
        createTaskTextRemovalRegexSetting(containerEl)
    }

    private fun createTaskTextRemovalRegexSetting(containerEl: HTMLElement): Setting {
        return Setting(containerEl)
            .setName("Task Text Removal Regex")
            .setDesc("Contents to remove from task on completion")
            .addText { text ->
                text.setPlaceholder("Regex")
                    .setValue(store.state.settings.taskRemoveRegex)
                    .onChange { value ->
                        console.log("Regex: $value")
                        store.state.settings.taskRemoveRegex = value
                        plugin.saveData(settingsService.toJson())
                    }
            }
    }

    private fun createColumnListSetting(containerEl: HTMLElement): Setting {
        return Setting(containerEl)
            .setName("Kanban Columns")
            .setDesc("List of columns to use for the Kanban board")
            .addTextArea { text ->
                val columns = store.state.settings.columnTags
                val textVersion =
                    columns.joinToString("\n") { statusTag -> "${statusTag.tag}:${statusTag.displayName}" }
                text.setValue(textVersion)
                    .onChange { value ->
                        val statusList = mutableListOf<StatusTag>()
                        value.split("\n").forEach { column ->
                            val tagValues = column.split(":")
                            statusList.add(StatusTag(tagValues[0], tagValues[1]))
                        }

                    }
            }
    }
}
