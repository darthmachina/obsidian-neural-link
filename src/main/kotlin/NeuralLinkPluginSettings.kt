@OptIn(ExperimentalJsExport::class)
@JsExport
data class NeuralLinkPluginSettings(
    var mySetting: String
) {
    companion object {
        fun default() : NeuralLinkPluginSettings{
            return NeuralLinkPluginSettings("default")
        }

        fun toJson(settings: NeuralLinkPluginSettings) : String {
            return JSON.stringify(settings)
        }

        fun fromJson(json: String) : NeuralLinkPluginSettings {
            return JSON.parse(json)
        }
    }
}