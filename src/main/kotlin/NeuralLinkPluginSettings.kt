import model.StatusTag

@OptIn(ExperimentalJsExport::class)
@JsExport
data class NeuralLinkPluginSettings(
    var version: Int,
    var taskRemoveRegex: String,
    var columnTags: MutableList<StatusTag>
) {
    companion object {
        fun default(): NeuralLinkPluginSettings {
            return NeuralLinkPluginSettings(
                2,
                """#kanban/[\w-]+(\s|$)""",
                mutableListOf(
                    StatusTag("backlog", "Backlog"),
                    StatusTag("inprogress", "In Progress"),
                    StatusTag("completed", "Completed")
                )
            )
        }
    }
}
