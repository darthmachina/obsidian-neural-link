import kotlinx.serialization.Serializable
import neurallink.core.model.StatusTag

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class NeuralLinkPluginSettings(
    val version: String,
    val taskRemoveRegex: String,
    val columnTags: List<StatusTag>,
    val tagColors: Map<String,String>
) {
    companion object {
        fun default(): NeuralLinkPluginSettings {
            return NeuralLinkPluginSettings(
                "3",
                """#kanban/[\w-]+(\s|$)""",
                listOf(
                    StatusTag("backlog", "Backlog"),
                    StatusTag("scheduled", "Scheduled", true),
                    StatusTag("inprogress", "In Progress"),
                    StatusTag("completed", "Completed")
                ),
                mapOf(
                    "personal" to "13088C",
                    "home" to "460A60",
                    "family" to "8E791C",
                    "marriage" to "196515",
                    "work" to "D34807"
                )
            )
        }
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class NeuralLinkPluginSettings2(
    val version: String,
    val taskRemoveRegex: String,
    val columnTags: List<StatusTag>
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class SettingsVersion(
    val version: String
)
