package event

import ModifiedTask
import NeuralLinkPlugin
import NeuralLinkState
import TFile
import processor.RepeatingProcessor
import processor.RemoveTagsFromTask
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
        RemoveTagsFromTask(state, taskService),
        RepeatingProcessor(state, taskService)
    ).sortedBy { it.getPriority() }

    private fun printMap(map: MutableMap<String, String>) : String {
        return map.map { (key, value) -> "[$key, $value]" }.joinToString(", ")
    }

    override fun processEvent(context: Any) {
        console.log("processEvent: ", context)
        if (context is TFile) {
            // Only mark as modified if the line was changed in some way so we only write the file if we need to
            var modified = false
            val fileContents = mutableListOf<String>()

            // Collect lines to delete from file. Needed mainly for indented items as they are included in the Task
            // markdown which replaces just the main task line in fileContents but are separate lines in the original
            // fileContents. No items are added to fileContents itself so these indices remain consistent even if
            // more than one task is processed.
            val linesToRemove = mutableListOf<Int>()

            plugin.app.vault.read(context).then { contents ->
                fileContents.addAll(contents.split('\n'))
                val fileListItems = plugin.app.metadataCache.getFileCache(context)?.listItems ?: arrayOf()
                val taskModel = taskService.buildTaskModel(fileContents, fileListItems)
                taskModel
                    .filter { (_, task) ->
                        task.completed
                    }
                    .forEach { (line, task) ->
                        val modifiedTask = ModifiedTask(task)

                        // TaskProcessors have side effects on modifiedTask
                        taskProcessors.forEach { processor ->
                            processor.processTask(modifiedTask)
                        }

                        if (modifiedTask.modified) {
                            console.log("Task modified, writing new contents to file", modifiedTask)
                            val totalLines =
                                modifiedTask.before.plus(modifiedTask.original).plus(modifiedTask.after)
                            fileContents[line] = totalLines.joinToString("\n") { it.toMarkdown() }
                            val indentedCount = taskService.indentedCount(modifiedTask.original)
                            if (indentedCount > 0) {
                                val firstIndent = line + 1
                                // Use 'until' as we don't include the last element (indentedCount includes the firstIndent line)
                                linesToRemove.addAll((firstIndent until (firstIndent + indentedCount)).toList())
                                console.log("linesToRemove now", linesToRemove)
                            }
                            modified = true
                        }
                    }

                if (modified) {
                    console.log("File was modified, writing new content")
                    // Remove the old indented lines from Tasks that were processed
                    // Sorted in descending order to maintain each index
                    linesToRemove.sortedDescending().forEach {
                        fileContents.removeAt(it)
                    }
                    plugin.app.vault.modify(context, fileContents.joinToString("\n"))
                }
            }
        }
    }
}
