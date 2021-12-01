@OptIn(ExperimentalJsExport::class)
@JsExport
data class NeuralLinkPluginSettings(
    var taskRemoveRegex: String
) {
    companion object {
        fun default() : NeuralLinkPluginSettings{
            return NeuralLinkPluginSettings("""#kanban/[\w-]+(\s|$)""")
        }

        fun toJson(settings: NeuralLinkPluginSettings) : String {
            return JSON.stringify(settings)
        }

        fun fromJson(json: String) : NeuralLinkPluginSettings {
            return JSON.parse(json)
        }
    }
}