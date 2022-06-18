package neurallink.core.event

import NeuralLinkPlugin
import neurallink.core.model.NeuralLinkModel
import org.reduxkotlin.Store

@OptIn(ExperimentalJsExport::class)
@JsExport
abstract class Event(val plugin: NeuralLinkPlugin, val store: Store<NeuralLinkModel>) {
    abstract fun processEvent(context: Any)
}
