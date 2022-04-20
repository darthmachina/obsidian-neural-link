import event.FileModifiedEvent
import kotlinx.coroutines.*
import model.FilterType
import model.TaskModel
import org.reduxkotlin.applyMiddleware
import org.reduxkotlin.createStore
import org.reduxkotlin.middleware
import service.SettingsService
import service.TaskModelService
import neurallink.core.store.reducer
import view.KanbanView

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("default")
class NeuralLinkPlugin(override var app: App, override var manifest: PluginManifest) : Plugin(app, manifest) {
    /**
     * Logs all actions and states after they are dispatched.
     */
    val loggerMiddleware = middleware<TaskModel> { store, next, action ->
        val result = next(action)
        console.log("DISPATCH action: ${action::class.simpleName}:", action)
        console.log("next state :", store.state)
        result
    }

    private val store = createStore(
        reducer,
        TaskModel(NeuralLinkPluginSettings.default(), mutableListOf(), mutableMapOf(), FilterType.NONE, ""),
        applyMiddleware(loggerMiddleware)
    ).apply {
        subscribe(::taskModifiedListener)
    }

    // Dependent classes are constructed here and passed into the classes that need them. Poor man's DI.
    // SERVICES
    private val settingsService = SettingsService(store, this)
    private val taskModelService = TaskModelService(store)
    private val repeatingTaskService = RepeatingTaskService()

    // EVENTS
    private val fileModifiedEvent = FileModifiedEvent(this, store, taskModelService, repeatingTaskService)

    override fun onload() {
        // TODO Need to wrap this around something so it's delayed on app startup
        loadSettingAndTaskModel()

        this.registerEvent(this.app.metadataCache.on("changed") { file ->
            fileModifiedEvent.processEvent(file)
        })

        // Add Settings tab
        addSettingTab(NeuralLinkPluginSettingsTab(app, this, settingsService, store))

        // Kanban View
        this.registerView(KanbanView.VIEW_TYPE) { leaf ->
            KanbanView(leaf, store, repeatingTaskService)
        }
        this.addCommand(KanbanViewCommand(
            "neural-link-kanban",
            "Open Neural Link Kanban") {
            activateView()
        })

        console.log("NeuralLinkPlugin onload()")
    }

    override fun onunload() {
        console.log("NeuralLinkPlugin.onunload()")
        this.app.workspace.detachLeavesOfType(KanbanView.VIEW_TYPE)
    }

    private fun taskModifiedListener() {
        console.log("NeuralLinkPlugin.taskModifiedListener()")
        CoroutineScope(Dispatchers.Main).launch {
            taskModelService.writeModifiedTasks(
                store.state.tasks,
                app.vault
            )
        }
    }

    private fun loadSettingAndTaskModel() {
        console.log("NeuralLinkPlugin.loadSettingAndTaskModel()")
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Default) { loadSettings() } // Load settings first and wait
            taskModelService.loadTasKModelIntoStore(
                app.vault,
                app.metadataCache,
                store
            )
        }
    }

    private suspend fun loadSettings() {
        loadData().then { result -> settingsService.loadFromJson(result) }.await()
    }

    private fun activateView() {
        console.log("NeuralLinkPlugin.activateView()")
        this.app.workspace.detachLeavesOfType(KanbanView.VIEW_TYPE)

        console.log(" - setting view state")
        val viewState = object : ViewState {
            override var type: String = KanbanView.VIEW_TYPE
        }
        console.log(" - getRightLeaf")
        this.app.workspace.getRightLeaf(false).setViewState(viewState).then {
            val leaf = this.app.workspace.getLeavesOfType(KanbanView.VIEW_TYPE)
            console.log(" - leaf found: [$leaf]")
            if (leaf.isNotEmpty()) {
                this.app.workspace.revealLeaf(leaf[0])
            }
        }
        console.log("activateView() end")
    }

    class KanbanViewCommand(
        override var id: String,
        override var name: String,
        override var callback: (() -> Any)?
    ) : Command
}
