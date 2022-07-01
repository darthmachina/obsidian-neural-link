import neurallink.core.event.FileModifiedEvent
import kotlinx.coroutines.*
import mu.KotlinLogging
import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel
import neurallink.core.event.FileCreatedEvent
import neurallink.core.event.FileDeletedEvent
import neurallink.core.model.NeuralLinkModel
import neurallink.core.service.loadFromJson
import neurallink.core.service.loadTasKModelIntoStore
import neurallink.core.service.writeModifiedTasks
import neurallink.core.settings.NeuralLinkPluginSettings5
import neurallink.core.settings.NeuralLinkPluginSettingsTab
import neurallink.core.store.NoneFilterValue
import neurallink.core.store.UpdateSettings
import org.reduxkotlin.applyMiddleware
import org.reduxkotlin.createStore
import org.reduxkotlin.middleware
import neurallink.core.store.reducer
import neurallink.core.view.KanbanView
import neurallink.core.view.ViewConstants

private val logger = KotlinLogging.logger("NeuralLinkPlugin")

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("default")
class NeuralLinkPlugin(override var app: App, override var manifest: PluginManifest) : Plugin(app, manifest) {
    /**
     * Logs all actions and states after they are dispatched.
     */
    val loggerMiddleware = middleware<NeuralLinkModel> { store, next, action ->
        logger.info { "DISPATCH action: ${action::class.simpleName}"  }
        logger.trace { action }
        val result = next(action)
        logger.trace { "next state :  ${store.state}" }
        result
    }

    private val store = createStore(
        reducer,
        NeuralLinkModel(
            this,
            NeuralLinkPluginSettings5.default(),
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
    private val fileDeletedEvent = FileDeletedEvent(this, store)
    private val fileCreatedEvent = FileCreatedEvent(this, store)

    override fun onload() {
        KotlinLoggingConfiguration.LOG_LEVEL = KotlinLoggingLevel.DEBUG
        // TODO Need to wrap this around something so it's delayed on app startup
        loadSettingAndTaskModel()

        this.registerEvent(this.app.metadataCache.on("changed") { file ->
            fileModifiedEvent.processEvent(file)
        })
        this.registerEvent(this.app.metadataCache.on("deleted") { file ->
            fileDeletedEvent.processEvent(file)
        })
        this.registerEvent(this.app.metadataCache.on("created") { file ->
            fileCreatedEvent.processEvent(file)
        })

        // Add Settings tab
        addSettingTab(NeuralLinkPluginSettingsTab(app, this, store))

        // Kanban View
        this.registerView(KanbanView.VIEW_TYPE) { leaf ->
            KanbanView(leaf, store)
        }
        this.addCommand(NeuralLinkCommand(
            "neural-link-kanban",
            "Open Neural Link Kanban") {
            activateView()
        })

        logger.debug { "NeuralLinkPlugin onload()" }
    }

    override fun onunload() {
        logger.debug { "NeuralLinkPlugin.onunload()" }
        this.app.workspace.detachLeavesOfType(KanbanView.VIEW_TYPE)
    }

    private fun taskModifiedListener() {
        logger.debug { "NeuralLinkPlugin.taskModifiedListener()" }
        CoroutineScope(Dispatchers.Main).launch {
            writeModifiedTasks(
                store.state.tasks,
                app.vault
            )
        }
    }

    private fun loadSettingAndTaskModel() {
        logger.debug { "NeuralLinkPlugin.loadSettingAndTaskModel()" }
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
                    Notice("ERROR loading settings JSON: ${it.message}", ViewConstants.NOTICE_TIMEOUT)
                    logger.error { "ERROR loading settings JSON: ${it.message}" }
                    it
                }
        }.await()
    }

    private fun activateView() {
        logger.debug { "NeuralLinkPlugin.activateView()" }
        this.app.workspace.detachLeavesOfType(KanbanView.VIEW_TYPE)

        val viewState = object : ViewState {
            override var type: String = KanbanView.VIEW_TYPE
        }
        this.app.workspace.getRightLeaf(false).setViewState(viewState).then {
            val leaf = this.app.workspace.getLeavesOfType(KanbanView.VIEW_TYPE)
            if (leaf.isNotEmpty()) {
                this.app.workspace.revealLeaf(leaf[0])
            }
        }
    }

    class NeuralLinkCommand(
        override var id: String,
        override var name: String,
        override var callback: (() -> Any)?
    ) : Command
}
