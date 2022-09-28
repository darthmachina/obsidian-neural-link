import kotlinx.coroutines.*
import mu.KotlinLogging
import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel
import neurallink.core.event.*
import neurallink.core.model.FilterOptions
import neurallink.core.model.NeuralLinkModel
import neurallink.core.model.StoreActions
import neurallink.core.service.loadFromJson
import neurallink.core.service.loadTasKModelIntoStore
import neurallink.core.service.writeModifiedTasks
import neurallink.core.settings.NeuralLinkPluginSettings6
import neurallink.core.settings.NeuralLinkPluginSettingsTab
import neurallink.core.store.UpdateSettings
import org.reduxkotlin.applyMiddleware
import org.reduxkotlin.createStore
import org.reduxkotlin.middleware
import neurallink.core.store.reducer
import neurallink.view.KanbanView
import neurallink.view.ViewConstants

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
            false,
            this,
            NeuralLinkPluginSettings6.default(),
            listOf(),
            FilterOptions(),
            listOf(),
            StoreActions.NOOP
        ),
        applyMiddleware(loggerMiddleware)
    ).apply {
        subscribe(::taskModifiedListener)
    }

    override fun onload() {
        KotlinLoggingConfiguration.LOG_LEVEL = KotlinLoggingLevel.DEBUG
        // TODO Need to wrap this around something so it's delayed on app startup
        app.workspace.onLayoutReady {
            logger.info { "Layout ready, loading settings and model"}
            loadSettingAndTaskModel()

            registerEvent(app.metadataCache.on(FileEventType.EVENT_MODIFIED.eventName) { file ->
                sendFileModifiedEvent(FileEventType.EVENT_MODIFIED, file)
            })
            registerEvent(app.metadataCache.on(FileEventType.EVENT_DELETED.eventName) { file ->
                sendFileModifiedEvent(FileEventType.EVENT_DELETED, file)
            })
            registerEvent(app.metadataCache.on(FileEventType.EVENT_CREATED.eventName) { file ->
                sendFileModifiedEvent(FileEventType.EVENT_CREATED, file)
            })

            // Add Settings tab
            addSettingTab(NeuralLinkPluginSettingsTab(app, this, store))

            // Kanban View
            registerView(KanbanView.VIEW_TYPE) { leaf ->
                KanbanView(leaf, store)
            }
            addCommand(NeuralLinkCommand(
                "neural-link-kanban",
                "Open Neural Link Kanban") {
                activateView()
            })

            CoroutineScope(Dispatchers.Default).launch {
                processFileEvents(app, store)
            }

            logger.debug { "NeuralLinkPlugin onload() finished" }
        }
    }

    override fun onunload() {
        logger.debug { "NeuralLinkPlugin.onunload()" }
        fileEventChannel.close()
        this.app.workspace.detachLeavesOfType(KanbanView.VIEW_TYPE)
    }

    private fun sendFileModifiedEvent(type: FileEventType, file: TFile) {
        CoroutineScope(Dispatchers.Default).launch {
            when (type) {
                FileEventType.EVENT_MODIFIED -> {
                    fileEventChannel.send(FileEventModified(file))
                }
                FileEventType.EVENT_CREATED -> {
                    fileEventChannel.send(FileEventCreated(file))
                }
                FileEventType.EVENT_DELETED -> {
                    fileEventChannel.send(FileEventDeleted(file))
                }
            }
        }
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
                            it.tagColors,
                            it.logLevel,
                            it.ignorePaths
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
