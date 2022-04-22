import kotlinx.serialization.Serializable
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class NeuralLinkPluginSettings(
    val version: String,
    val taskRemoveRegex: String,
    val columnTags: List<StatusTag>,
    val tagColors: Map<Tag,String>
) {
    companion object {
        fun default(): NeuralLinkPluginSettings {
            return NeuralLinkPluginSettings(
                "4",
                """#kanban/[\w-]+(\s|$)""",
                listOf(
                    StatusTag(Tag("backlog"), "Backlog"),
                    StatusTag(Tag("scheduled"), "Scheduled", true),
                    StatusTag(Tag("inprogress"), "In Progress"),
                    StatusTag(Tag("completed"), "Completed")
                ),
                mapOf(
                    Tag("personal") to "13088C",
                    Tag("home") to "460A60",
                    Tag("family") to "8E791C",
                    Tag("marriage") to "196515",
                    Tag("work") to "D34807"
                )
            )
        }
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class SettingsVersion(
    val version: String
)
