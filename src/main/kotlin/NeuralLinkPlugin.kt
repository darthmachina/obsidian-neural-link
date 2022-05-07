import neurallink.core.event.FileModifiedEvent
import kotlinx.coroutines.*
import neurallink.core.model.NeuralLinkModel
import neurallink.core.service.loadFromJson
import neurallink.core.service.loadTasKModelIntoStore
import neurallink.core.service.writeModifiedTasks
import neurallink.core.store.NoneFilterValue
import neurallink.core.store.UpdateSettings
import org.reduxkotlin.applyMiddleware
import org.reduxkotlin.createStore
import org.reduxkotlin.middleware
import neurallink.core.store.reducer
import view.KanbanView

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("default")
class NeuralLinkPlugin(override var app: App, override var manifest: PluginManifest) : Plugin(app, manifest) {
    /**
     * Logs all actions and states after they are dispatched.
     */
    val loggerMiddleware = middleware<NeuralLinkModel> { store, next, action ->
        val result = next(action)
        console.log("DISPATCH action: ${action::class.simpleName}:", action)
        console.log("next state :", store.state)
        result
    }

    private val store = createStore(
        reducer,
        NeuralLinkModel(
            this,
            NeuralLinkPluginSettings.default(),
            listOf(),
            mapOf(),
            NoneFilterValue()
        ),
        applyMiddleware(loggerMiddleware)
    ).apply {
        subscribe(::taskModifiedListener)
    }

    // EVENTS
    private val fileModifiedEvent = FileModifiedEvent(this, store)

    override fun onload() {
        // TODO Need to wrap this around something so it's delayed on app startup
        loadSettingAndTaskModel()

        this.registerEvent(this.app.metadataCache.on("changed") { file ->
            fileModifiedEvent.processEvent(file)
        })

        // Add Settings tab
        addSettingTab(NeuralLinkPluginSettingsTab(app, this, store))

        // Kanban View
        this.registerView(KanbanView.VIEW_TYPE) { leaf ->
            KanbanView(leaf, store)
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
            writeModifiedTasks(
                store.state.tasks,
                app.vault
            )
        }
    }

    private fun loadSettingAndTaskModel() {
        console.log("NeuralLinkPlugin.loadSettingAndTaskModel()")
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Default) { loadSettings() } // Load settings first and wait
            loadTasKModelIntoStore(
                app.vault,
                app.metadataCache,
                store
            )
        }
    }

    private suspend fun loadSettings() {
        loadData().then { result ->
            loadFromJson(result)
                .map {
                    store.dispatch(
                        UpdateSettings(
                            store.state.plugin,
                            it.taskRemoveRegex,
                            it.columnTags,
                            it.tagColors
                        )
                    )
                }
                .mapLeft {
                    console.log("ERROR loading settings JSON")
                    it
                }
        }.await()
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
