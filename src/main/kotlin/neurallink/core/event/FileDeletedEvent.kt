package neurallink.core.event

import NeuralLinkPlugin
import TFile
import mu.KotlinLogging
import neurallink.core.model.NeuralLinkModel
import neurallink.core.model.TaskFile
import neurallink.core.store.FileDeleted
import org.reduxkotlin.Store

private val logger = KotlinLogging.logger("FleDeletedEvent")

class FileDeletedEvent(
    plugin: NeuralLinkPlugin,
    store: Store<NeuralLinkModel>
) : Event(plugin, store) {
    override fun processEvent(context: Any) {
        logger.debug { "processEvent(): $context" }
        if (context is TFile) {
            store.dispatch(FileDeleted(TaskFile(context.path)))
        }
    }
}