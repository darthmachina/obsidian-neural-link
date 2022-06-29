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
import neurallink.core.store.FileCreated
import org.reduxkotlin.Store

private val logger = KotlinLogging.logger("FleCreatedEvent")

class FileCreatedEvent(
    plugin: NeuralLinkPlugin,
    store: Store<NeuralLinkModel>
) : Event(plugin, store) {
    override fun processEvent(context: Any) {
        logger.debug { "processEvent(): $context" }
        if (context is TFile) {
            CoroutineScope(Dispatchers.Main).launch {
                val tasks = readFile(store, context, plugin.app.vault, plugin.app.metadataCache)
                store.dispatch(FileCreated(TaskFile(context.path), tasks))
            }
        }
    }
}