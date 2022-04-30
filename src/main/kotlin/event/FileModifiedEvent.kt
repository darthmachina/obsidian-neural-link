package event

import NeuralLinkPlugin
import TFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import model.TaskModel
import neurallink.core.model.TaskFile
import neurallink.core.service.readFile
import org.reduxkotlin.Store
import service.RepeatingTaskService
import store.ModifyFileTasks

/**
 * Meant to be called when a file is modified (usually from the MetadataCache). This event happens a LOT, so this
 * handler needs to be *very* quick to not cause performance issues when typing.
 */
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class FileModifiedEvent(
    plugin: NeuralLinkPlugin,
    store: Store<TaskModel>,
    val repeatingTaskService: RepeatingTaskService
) : Event(plugin, store) {
    override fun processEvent(context: Any) {
        console.log("processEvent()", context)
        if (context is TFile) {
            val fileContents = mutableListOf<String>()
            plugin.app.vault.read(context).then { contents ->
                fileContents.addAll(contents.split("\n"))
                CoroutineScope(Dispatchers.Main).launch {
                    val tasks = readFile(store, context, plugin.app.vault, plugin.app.metadataCache)
                    store.dispatch(ModifyFileTasks(TaskFile(context.path), tasks, repeatingTaskService))
                }
            }
        }
    }
}
