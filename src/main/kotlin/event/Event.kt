package event

import NeuralLinkPlugin
import model.TaskModel
import org.reduxkotlin.Store

@OptIn(ExperimentalJsExport::class)
@JsExport
abstract class Event(val plugin: NeuralLinkPlugin, val store: Store<TaskModel>) {
    abstract fun processEvent(context: Any)
}
