import kotlinx.html.dom.append
import kotlinx.html.js.h2
import model.StatusTag
import model.TaskModel
import org.reduxkotlin.Store
import org.w3c.dom.HTMLElement
import service.SettingsService
import store.UpdateSettings

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
        createColumnListSetting(containerEl)
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
                        store.dispatch(UpdateSettings(plugin, settingsService, taskRemoveRegex = value))
                    }
            }
    }

    private fun createColumnListSetting(containerEl: HTMLElement): Setting {
        console.log("createColumnListSetting()")
        return Setting(containerEl)
            .setName("Kanban Columns")
            .setDesc("List of columns to use for the Kanban board")
            .addTextArea { text ->
                val columns = store.state.settings.columnTags
                console.log(" - current columns", columns)
                val stringList = columns.map { statusTag -> "${statusTag.tag}:${statusTag.displayName}" }
                console.log(" - stringList", stringList)
                val textVersion = stringList.joinToString("\n")
                console.log(" - textVersion", textVersion)
                text.setPlaceholder("'tag:display name' separated by newlines")
                    .setValue(textVersion)
                    .onChange { value ->
                        val statusList = mutableListOf<StatusTag>()
                        value.split("\n").forEach { column ->
                            val tagValues = column.split(":")
                            statusList.add(StatusTag(tagValues[0], tagValues[1]))
                        }
                        store.dispatch(UpdateSettings(plugin, settingsService, columnTags = statusList))
                    }
            }
    }
}
