@OptIn(ExperimentalJsExport::class)
@JsExport
data class NeuralLinkPluginSettings(
    var version: Int,
    var taskRemoveRegex: String
) {
    companion object {
        fun default() : NeuralLinkPluginSettings{
            return NeuralLinkPluginSettings(
                1,
                """#kanban/[\w-]+(\s|$)"""
            )
        }

        fun toJson(settings: NeuralLinkPluginSettings) : String {
            return JSON.stringify(settings)
        }

        fun fromJson(json: String) : NeuralLinkPluginSettings {
            return JSON.parse(json)
        }
    }
}