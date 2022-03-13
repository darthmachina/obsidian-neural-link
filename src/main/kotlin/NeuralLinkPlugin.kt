import kotlinx.coroutines.*
import model.TaskModel
import org.reduxkotlin.Store
import org.reduxkotlin.applyMiddleware
import org.reduxkotlin.createStore
import org.reduxkotlin.middleware
import service.SettingsService
import service.TaskModelService
import service.TaskService
import store.reducer
import view.KanbanView

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("default")
class NeuralLinkPlugin(override var app: App, override var manifest: PluginManifest) : Plugin(app, manifest) {
    /**
     * Logs all actions and states after they are dispatched.
     */
    val loggerMiddleware = middleware<TaskModel> { store, next, action ->
        console.log("INCOMING STATE: ", store.state)
        store.state.kanbanColumns.keys.forEach { status ->
            console.log(" - incoming task list for $status : ", store.state.kanbanColumns[status]!!)
        }

        val result = next(action)
        console.log("DISPATCH action: ${action::class.simpleName}: $action")
        console.log("next state :", store.state)
        store.state.kanbanColumns.keys.forEach { status ->
            console.log(" - next task list for $status : ", store.state.kanbanColumns[status]!!)
        }
        result
    }

    private val store = createStore(
        reducer,
        TaskModel(NeuralLinkPluginSettings.default(), mutableListOf(), mutableMapOf()),
        applyMiddleware(loggerMiddleware)
    )

    // Dependent classes are constructed here and passed into the classes that need them. Poor man's DI.
    // SERVICES
    private val settingsService = SettingsService(store)
    private val taskService = TaskService()
    private val taskModelService = TaskModelService()

    // EVENTS
//    private val fileModifiedEvent = FileModifiedEvent(this, state, taskService)

    override fun onload() {
//        this.app.workspace.onLayoutReady {
//            // TODO Need to check if it's loaded already, I think
//            loadTaskModel()
//        }
        // TODO Need to wrap this around something so it's delayed on app startup
        loadSettingAndTaskModel()

//        this.registerEvent(this.app.metadataCache.on("changed") { file ->
//            fileModifiedEvent.processEvent(file)
//        })

        // Add Settings tab
        addSettingTab(NeuralLinkPluginSettingsTab(app, this, settingsService, store))

        // Kanban View
        this.registerView(KanbanView.VIEW_TYPE) { leaf ->
            KanbanView(leaf, store, taskModelService)
        }
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

    private fun loadSettingAndTaskModel() {
        console.log("loadSettingAndTaskModel()")
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
        console.log("activateView()")
        this.app.workspace.detachLeavesOfType(KanbanView.VIEW_TYPE)

        console.log("activateView() setting view state")
        val viewState = object : ViewState {
            override var type: String = KanbanView.VIEW_TYPE
        }
        console.log("activateView() getRightLeaf")
        this.app.workspace.getRightLeaf(false).setViewState(viewState).then {
            val leaf = this.app.workspace.getLeavesOfType(KanbanView.VIEW_TYPE)
            console.log("leaf found: [$leaf]")
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
