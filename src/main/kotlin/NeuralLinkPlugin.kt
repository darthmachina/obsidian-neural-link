import event.FileModifiedEvent
import service.SettingsService
import service.TaskService
import view.KanbanView

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

        // Kanban View
        this.registerView(KanbanView.VIEW_TYPE, ::KanbanView)
        this.addCommand(KanbanViewCommand(
            "neural-link-kanban",
            "Open Neural Link Kanban") {
            activateView()
        })

        console.log("NeuralLinkPlugin onload()")
    }

    override fun onunload() {
        console.log("NeuralLinkPlugin onunload()")
    }

    private fun loadSettings() {
        loadData().then { result -> settingsService.loadFromJson(result) }
    }

    private fun activateView() {
        console.log("activateView()")
        this.app.workspace.detachLeavesOfType(KanbanView.VIEW_TYPE)

        console.log("activateView() setting view state")
        val viewState = object : ViewState {
            override var type: String = KanbanView.VIEW_TYPE
        }
        console.log("activateView() getRightLeaf")
        this.app.workspace.getRightLeaf(false).setViewState(viewState)
        val leaf = this.app.workspace.getLeavesOfType(KanbanView.VIEW_TYPE)
        console.log("leaf found: [$leaf]")
        this.app.workspace.revealLeaf(leaf[0])
        console.log("activateView() end")
    }

    class KanbanViewCommand(override var id: String, override var name: String, override var callback: (() -> Any)?) : Command {}
}