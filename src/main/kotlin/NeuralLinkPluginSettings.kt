import kotlinx.serialization.Serializable
import model.StatusTag

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class NeuralLinkPluginSettings(
    val taskRemoveRegex: String,
    val columnTags: List<StatusTag>,
    val version: Int = 2
) {
    companion object {
        fun default(): NeuralLinkPluginSettings {
            return NeuralLinkPluginSettings(
                """#kanban/[\w-]+(\s|$)""",
                listOf(
                    StatusTag("backlog", "Backlog"),
                    StatusTag("inprogress", "In Progress"),
                    StatusTag("completed", "Completed")
                )
            )
        }
    }
}
