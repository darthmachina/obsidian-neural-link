package event

import NeuralLinkPlugin

@OptIn(ExperimentalJsExport::class)
@JsExport
abstract class Event(val plugin: NeuralLinkPlugin) {
    abstract fun processEvent(context: Any)
}
