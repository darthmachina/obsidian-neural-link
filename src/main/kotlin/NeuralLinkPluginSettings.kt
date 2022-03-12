@OptIn(ExperimentalJsExport::class)
@JsExport
data class NeuralLinkPluginSettings(
    var version: Int,
    var taskRemoveRegex: String,
    var columnTags: MutableList<String>
) {
    companion object {
        fun default(): NeuralLinkPluginSettings {
            return NeuralLinkPluginSettings(
                2,
                """#kanban/[\w-]+(\s|$)""",
                mutableListOf("backlog", "inprogress", "completed")
            )
        }
    }
}
