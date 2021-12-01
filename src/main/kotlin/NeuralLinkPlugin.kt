import events.RemoveRegexFromTask
import events.TaskProcessor

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("default")
class NeuralLinkPlugin(override var app: App, override var manifest: PluginManifest) : Plugin(app, manifest) {
    var settings : NeuralLinkPluginSettings = NeuralLinkPluginSettings.default()

    val taskProcessors : List<TaskProcessor>

    init {
        taskProcessors = mutableListOf(RemoveRegexFromTask(this))
    }

    override fun onload() {
        loadSettings()

//        this.registerEvent(this.app.metadataCache.on("changed",
//            (file) => this.handleFileModified(file)));

        // Add Settings tab
        addSettingTab(NeuralLinkPluginSettingsTab(app, this))
        console.log("KotlinPlugin onload()")
    }

    override fun onunload() {
        console.log("KotlinPlugin onunload()")
    }

    private fun handleFileModified(file: TAbstractFile) {

    }

    private fun loadSettings() {
        // TODO: implement exmaple of versioned settings
        loadData().then {result ->
            val loadedSettings = NeuralLinkPluginSettings.fromJson(result as String)
            console.log("loadedSettings: ", loadedSettings)
            // TODO Replace with a version check
            if (loadedSettings.mySetting != "") {
                console.log("Saving loaded settings")
                settings = loadedSettings
            }
        }
    }
}
