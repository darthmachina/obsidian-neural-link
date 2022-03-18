import model.StatusTag

@OptIn(ExperimentalJsExport::class)
@JsExport
data class NeuralLinkPluginSettings(
    val version: Int,
    val taskRemoveRegex: String,
    val columnTags: List<StatusTag>
) {
    companion object {
        fun default(): NeuralLinkPluginSettings {
            return NeuralLinkPluginSettings(
                2,
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
