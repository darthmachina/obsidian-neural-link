package neurallink.core.event

import NeuralLinkPlugin
import TFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import neurallink.core.model.NeuralLinkModel
import neurallink.core.model.TaskFile
import neurallink.core.service.readFile
import neurallink.core.store.ModifyFileTasks
import org.reduxkotlin.Store

private val logger = KotlinLogging.logger("NeuralLinkPlugin")

/**
 * Meant to be called when a file is modified (usually from the MetadataCache). This event happens a LOT, so this
 * handler needs to be *very* quick to not cause performance issues when typing.
 */
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class FileModifiedEvent(
    plugin: NeuralLinkPlugin,
    store: Store<NeuralLinkModel>
) : Event(plugin, store) {
    override fun processEvent(context: Any) {
        logger.debug { "processEvent(): $context" }
        if (context is TFile) {
            val fileContents = mutableListOf<String>()
            plugin.app.vault.read(context).then { contents ->
                fileContents.addAll(contents.split("\n"))
                CoroutineScope(Dispatchers.Main).launch {
                    val tasks = readFile(store, context, plugin.app.vault, plugin.app.metadataCache)
                    store.dispatch(ModifyFileTasks(TaskFile(context.path), tasks))
                }
            }
        }
    }
}
