import kotlinx.serialization.Serializable
import model.StatusTag

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
                    "personal" to "2196F3",
                    "home" to "AB47BC",
                    "family" to "66BB6A",
                    "marriage" to "66BB6A",
                    "work" to "FFA726"
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
