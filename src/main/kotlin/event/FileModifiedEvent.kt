package event

import ModifiedTask
import NeuralLinkPlugin
import NeuralLinkState
import TFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import model.TaskConstants
import model.TaskModel
import org.reduxkotlin.Store
import processor.RepeatingProcessor
import processor.RemoveTagsFromTask
import service.RepeatingTaskService
import service.TaskModelService
import service.TaskService
import store.ModifyFileTasks
import store.RepeatTask

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
    val taskModelService: TaskModelService,
    val repeatingTaskService: RepeatingTaskService
) : Event(plugin, store) {
    @Suppress("NON_EXPORTABLE_TYPE")

    private fun printMap(map: MutableMap<String, String>) : String {
        return map.map { (key, value) -> "[$key, $value]" }.joinToString(", ")
    }

    override fun processEvent(context: Any) {
        console.log("processEvent()", context)
        if (context is TFile) {
            val fileContents = mutableListOf<String>()
            plugin.app.vault.read(context).then { contents ->
                fileContents.addAll(contents.split("\n"))
                CoroutineScope(Dispatchers.Main).launch {
                    val tasks = taskModelService.readFile(context, plugin.app.vault, plugin.app.metadataCache)
                    // Check for any completed tasks that are set to repeat
                    tasks
                        .filter { task ->
                            task.dataviewFields.keys.contains(TaskConstants.TASK_REPEAT_PROPERTY) &&
                            task.completed
                        }
                        .forEach { task ->
                            store.dispatch(RepeatTask(task.id, repeatingTaskService))
                        }

                    store.dispatch(ModifyFileTasks(context.path, tasks))
                }
            }
        }
    }

//    fun processEventOld(context: Any) {
//        console.log("processEventOld()", context)
//        if (context is TFile) {
//            // Only mark as modified if the line was changed in some way so we only write the file if we need to
//            var modified = false
//            val fileContents = mutableListOf<String>()
//
//            // Collect lines to delete from file. Needed mainly for indented items as they are included in the Task
//            // markdown which replaces just the main task line in fileContents but are separate lines in the original
//            // fileContents. No items are added to fileContents itself so these indices remain consistent even if
//            // more than one task is processed.
//            val linesToRemove = mutableListOf<Int>()
//
//            plugin.app.vault.read(context).then { contents ->
//                fileContents.addAll(contents.split('\n'))
//                val fileListItems = plugin.app.metadataCache.getFileCache(context)?.listItems ?: arrayOf()
//                val taskModel = taskService.buildTaskModel(fileContents, fileListItems)
//                taskModel
//                    .filter { (_, task) ->
//                        task.completed
//                    }
//                    .forEach { (line, task) ->
//                        val modifiedTask = ModifiedTask(task)
//
//                        // TaskProcessors have side effects on modifiedTask
//                        taskProcessors.forEach { processor ->
//                            modified = modified || processor.processTask(modifiedTask) // Keep modified true if it's already true
//                        }
//
//                        if (modified) {
//                            console.log("Task modified, writing new contents to file", modifiedTask)
//                            val totalLines =
//                                modifiedTask.before.plus(modifiedTask.original).plus(modifiedTask.after)
//                            fileContents[line] = totalLines.joinToString("\n") { it.toMarkdown() }
//                            val indentedCount = taskService.indentedCount(modifiedTask.original)
//                            if (indentedCount > 0) {
//                                val firstIndent = line + 1
//                                // Use 'until' as we don't include the last element (indentedCount includes the firstIndent line)
//                                linesToRemove.addAll((firstIndent until (firstIndent + indentedCount)).toList())
//                                console.log("linesToRemove now", linesToRemove)
//                            }
//                        }
//                    }
//
//                if (modified) {
//                    console.log("File was modified, writing new content")
//                    // Remove the old indented lines from Tasks that were processed
//                    // Sorted in descending order to maintain each index
//                    linesToRemove.sortedDescending().forEach {
//                        fileContents.removeAt(it)
//                    }
//                    plugin.app.vault.modify(context, fileContents.joinToString("\n"))
//                }
//            }
//        }
//    }
}
