@OptIn(ExperimentalJsExport::class)
@JsExport
data class NeuralLinkPluginSettings(
    var version: Int,
    var taskRemoveRegex: String
) {
    companion object {
        fun default(): NeuralLinkPluginSettings {
            return NeuralLinkPluginSettings(
                1,
                """#kanban/[\w-]+(\s|$)"""
            )
        }
    }
}
