import kotlinx.html.dom.append
import kotlinx.html.js.h2
import neurallink.core.model.StatusTag
import neurallink.core.model.NeuralLinkModel
import neurallink.core.model.Tag
import neurallink.core.store.UpdateSettings
import org.reduxkotlin.Store
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalJsExport::class)
@JsExport
class NeuralLinkPluginSettingsTab(
    override var app: App,
    private var plugin: NeuralLinkPlugin,
    private val store: Store<NeuralLinkModel>
) : PluginSettingTab(app, plugin) {
    override fun display() {
        while (containerEl.firstChild != null) {
            containerEl.lastChild?.let { containerEl.removeChild(it) }
        }

        containerEl.append.h2 { +"Neural Link Settings." }
        createTaskTextRemovalRegexSetting(containerEl)
        createColumnListSetting(containerEl)
        createTagColorListSetting(containerEl)
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
                        store.dispatch(UpdateSettings(plugin, taskRemoveRegex = value))
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
                val stringList = columns.map { statusTag -> "${statusTag.tag}:${statusTag.displayName}:${statusTag.dateSort}" }
                console.log(" - stringList", stringList)
                val textVersion = stringList.joinToString("\n")
                console.log(" - textVersion", textVersion)
                text.setPlaceholder("'tag:display name:dateSort' separated by newlines")
                    .setValue(textVersion)
                    .onChange { value ->
                        val statusList = mutableListOf<StatusTag>()
                        value.split("\n").forEach { column ->
                            val tagValues = column.split(":")
                            statusList.add(
                                StatusTag(
                                Tag(tagValues[0]),
                                tagValues[1],
                                if (tagValues.size == 3) tagValues[2].toBoolean() else false
                            )
                            )
                        }
                        store.dispatch(UpdateSettings(plugin, columnTags = statusList))
                    }
            }
    }

    private fun createTagColorListSetting(containerEl: HTMLElement): Setting {
        console.log("createTagColorListSetting()")
        return Setting(containerEl)
            .setName("Tag Colors")
            .setDesc("List of colors to use for certain tags")
            .addTextArea { text ->
                val tagColors = store.state.settings.tagColors
                console.log(" - current tagColors", tagColors)
                val stringList = tagColors.map { entry -> "${entry.key}:${entry.value}" }
                console.log(" - stringList", stringList)
                val textVersion = stringList.joinToString("\n")
                console.log(" - textVersion", textVersion)
                text.setPlaceholder("'tag:hex_color' separated by newlines")
                    .setValue(textVersion)
                    .onChange { value ->
                        val tagColors = mutableMapOf<Tag,String>()
                        value.split("\n").forEach { tagColor ->
                            val singleValues = tagColor.split(":")
                            tagColors[Tag(singleValues[0])] = singleValues[1]
                        }
                        store.dispatch(UpdateSettings(plugin, tagColors = tagColors))
                    }
            }
    }
}
