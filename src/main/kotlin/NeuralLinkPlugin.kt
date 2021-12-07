import event.FileModifiedEvent
import grammar.RecurringGrammar
import service.SettingsService
import service.TaskService

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("default")
class NeuralLinkPlugin(override var app: App, override var manifest: PluginManifest) : Plugin(app, manifest) {
    private val state = NeuralLinkState(NeuralLinkPluginSettings.default())

    // Dependent classes are constructed here and passed into the classes that need them. Poor man's DI.
    // SERVICES
    private val settingsService = SettingsService(state)
    private val taskService = TaskService()

    // EVENTS
    private val fileModifiedEvent = FileModifiedEvent(this, state, taskService)

    override fun onload() {
        loadSettings()

        this.registerEvent(this.app.metadataCache.on("changed") { file ->
            fileModifiedEvent.processEvent(file)
        })

        // Add Settings tab
        addSettingTab(NeuralLinkPluginSettingsTab(app, this, settingsService, state))
        console.log("NeuralLinkPlugin onload()")
    }

    private fun testStuff() {
        val grammar = RecurringGrammar()
        console.log(grammar.parse("monthly!: 3"))
        console.log(grammar.parse("daily: 1"))
        console.log(grammar.parse("jan: 24"))
    }

    override fun onunload() {
        console.log("NeuralLinkPlugin onunload()")
    }

    private fun loadSettings() {
        loadData().then { result -> settingsService.loadFromJson(result) }
    }
}
