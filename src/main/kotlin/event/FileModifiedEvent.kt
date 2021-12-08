package event

import NeuralLinkPlugin
import NeuralLinkState
import TFile
import processor.RecurringProcessor
import processor.RemoveRegexFromTask
import service.ModifiedTask
import service.TaskService

/**
 * Meant to be called when a file is modified (usually from the MetadataCache). This event happens a LOT, so this
 * handler needs to be *very* quick to not cause performance issues when typing.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class FileModifiedEvent(plugin: NeuralLinkPlugin, state: NeuralLinkState, taskService: TaskService) : Event(plugin) {
    @Suppress("NON_EXPORTABLE_TYPE")
    val taskProcessors = listOf(
        RemoveRegexFromTask(state),
        RecurringProcessor(state, taskService)
    ).sortedBy { it.getPriority() }

    override fun processEvent(context: Any) {
        console.log("processEvent: ", context)
        if (context is TFile) {
            // Only mark as modified if the line was changed in some way so we only write the file is we need to
            var modified = false
            val fileContents = mutableListOf<String>()
            plugin.app.vault.read(context).then { contents ->
                fileContents.addAll(contents.split('\n'))
                val fileListItems = plugin.app.metadataCache.getFileCache(context)?.listItems ?: arrayOf()
                fileListItems.forEach { listItem ->
                    if (listItem.task?.uppercase() == "X") {
                        var lineContents = ModifiedTask(fileContents[listItem.position.start.line.toInt()])
                        // Pass the task line through all the configured TaskProcessors
                        taskProcessors.forEach { processor ->
                            console.log("taskProcessors lineContents: ", lineContents.original)
                            lineContents = processor.processTask(lineContents)
                        }

                        if (lineContents.original != fileContents[listItem.position.start.line.toInt()]
                            || lineContents.before.isNotEmpty()
                            || lineContents.after.isNotEmpty()
                        ) {
                            val totalLines = lineContents.before.plus(lineContents.original).plus(lineContents.after)
                            fileContents[listItem.position.start.line.toInt()] = totalLines.joinToString("\n")
                            modified = true
                        }
                    }
                }

                if (modified) {
                    plugin.app.vault.modify(context, fileContents.joinToString("\n"))
                }
            }
        }
    }
}
