package event

import NeuralLinkPlugin
import model.NeuralLinkModel
import org.reduxkotlin.Store

@OptIn(ExperimentalJsExport::class)
@JsExport
abstract class Event(val plugin: NeuralLinkPlugin, val store: Store<NeuralLinkModel>) {
    abstract fun processEvent(context: Any)
}
