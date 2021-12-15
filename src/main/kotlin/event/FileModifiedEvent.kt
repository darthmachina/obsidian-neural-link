package event

import ModifiedTask
import NeuralLinkPlugin
import NeuralLinkState
import TFile
import processor.RecurringProcessor
import processor.RemoveRegexFromTask
import service.TaskService

/**
 * Meant to be called when a file is modified (usually from the MetadataCache). This event happens a LOT, so this
 * handler needs to be *very* quick to not cause performance issues when typing.
 */
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class FileModifiedEvent(plugin: NeuralLinkPlugin, state: NeuralLinkState, val taskService: TaskService) :
    Event(plugin) {
    @Suppress("NON_EXPORTABLE_TYPE")
    val taskProcessors = listOf(
        RemoveRegexFromTask(state, taskService),
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
                val taskModel = taskService.buildTaskModel(fileContents, fileListItems)
                console.log("taskModel: ", taskModel)
                taskModel.forEach { (line, task) ->
                    console.log("Task at line $line: ${task.toMarkdown()}")
                }
                console.log("taskModel size ${taskModel.size}", taskModel)
                taskModel
                    .filter { (_, task) ->
                        if (taskModel.size > 4) false
                        else
                            task.completed
                    }
                    .forEach { (line, task) ->
                        var modifiedTask = ModifiedTask(task)
                        console.log("Before processing: ", modifiedTask)
                        taskProcessors.forEach { processor ->
                            console.log("taskProcessors lineContents: ", modifiedTask.original)
                            modifiedTask = processor.processTask(modifiedTask)
                        }
                        console.log("After processing: ", modifiedTask)

                        if (modifiedTask.modified) {
                            val totalLines =
                                modifiedTask.before.plus(modifiedTask.original).plus(modifiedTask.after)
                            fileContents[line] = totalLines.joinToString("\n") { it.toMarkdown() }
                            modified = true
                        }
                    }

                if (modified) {
                    console.log("File was modified, writing new content")
                    plugin.app.vault.modify(context, fileContents.joinToString("\n"))
                }
            }
        }
    }
}
