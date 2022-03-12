import model.TaskModel
import org.reduxkotlin.applyMiddleware
import org.reduxkotlin.createStore
import org.reduxkotlin.middleware
import service.SettingsService
import service.TaskModelService
import service.TaskService
import store.reducer

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("default")
class NeuralLinkPlugin(override var app: App, override var manifest: PluginManifest) : Plugin(app, manifest) {
    /**
     * Logs all actions and states after they are dispatched.
     */
    val loggerMiddleware = middleware<TaskModel> { store, next, action ->
        val result = next(action)
        console.log("DISPATCH action: ${action::class.simpleName}: $action")
        console.log("next state: ${store.state}")
        result
    }

    private val store = createStore(
        reducer,
        TaskModel(NeuralLinkPluginSettings.default()),
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
        loadSettings()

//        this.registerEvent(this.app.metadataCache.on("changed") { file ->
//            fileModifiedEvent.processEvent(file)
//        })

        this.app.workspace.onLayoutReady {
            // TODO Need to check if it's loaded already, I think
//            loadTaskModel()
        }
        // TODO Need to wrap this around something so it's delayed on app startup
        // TODO Need to wait on the settings to load before this happens
        loadTaskModel()

        // Add Settings tab
        addSettingTab(NeuralLinkPluginSettingsTab(app, this, settingsService, store))
        console.log("NeuralLinkPlugin onload()")
    }

    override fun onunload() {
        console.log("NeuralLinkPlugin onunload()")
    }

    private fun loadTaskModel() {
        console.log("loadTaskModel()")
        val taskModel = taskModelService.loadTasKModel(
            this.app.vault,
            this.app.metadataCache,
            store
        )
    }

    private fun loadSettings() {
        loadData().then { result -> settingsService.loadFromJson(result) }
    }
}
