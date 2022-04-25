package neurallink.core.service

import ListItemCache
import MetadataCache
import TFile
import Vault
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import model.TaskModel
import neurallink.core.model.*
import org.reduxkotlin.Store

/**
 * Processes all files in the Vault and loads any tasks into the TaskModel.
 *
 * NOTE: This makes changes to the TaskModel so it assumes that the Redux Store has already been copied into a
 * new state
 *
 * @param vault The Obsidian Vault to process
 * @param metadataCache Obsidian cache
 * @return A filled TaskModel. This is the same instance as the taskModel parameter
 */
suspend fun processAllFiles(store: Store<TaskModel>, vault: Vault, metadataCache: MetadataCache): List<Task>
        = coroutineScope {
    console.log("processAllFiles()")
    vault.getFiles()
        .filter {
            it.name.endsWith(".md")
        }
        .filter { file ->
            val listItems = metadataCache.getFileCache(file)?.listItems?.toList() ?: emptyList()
            listItems.any { listItemCache ->
                listItemCache.task != null
            }
        }
        .map { file ->
            async {
                readFile(store, file, vault, metadataCache)
            }
        }.awaitAll()
        .flatten()
}

suspend fun readFile(store: Store<TaskModel>, file: TFile, vault: Vault, metadataCache: MetadataCache): List<Task> {
    console.log("readFile()", file.name)
    val taskList = mutableListOf<Task>()

    vault.read(file).then { contents ->
        val fileContents = contents.split('\n')
        val fileListItems = metadataCache.getFileCache(file)?.listItems ?: arrayOf()
        // Process the file and then filter according to these rules:
        // - not completed
        // - contains a repeat Dataview field
        // - contains a status tag
        // Rules for completed tasks are so they are processed by any modification listeners for tasks modified
        // outside the plugin itself.
        val tasksForFile = processFile(file.path, fileContents, fileListItems)
            .filter { task ->
                !task.completed ||
                    task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)) ||
                    task.tags.any { tag -> tag in store.state.settings.columnTags.map { it.tag } }
        }
        taskList.addAll(tasksForFile)
    }.await()
    return taskList}

fun processFile(
    filename: String,
    fileContents: List<String>,
    listItems: Array<ListItemCache>
): List<Task> {
    console.log("processFile()", filename)
    val listItemsByLine = mutableMapOf<Int, ListItem>() // Map of position -> Task

    listItems
        .map { cacheListItem ->
            if (cacheListItem.parent.toInt() < 0 || !listItemsByLine.contains(cacheListItem.parent.toInt())) {
                // Root level list item
                if (cacheListItem.task != null) {
                    // Only care about root items that are tasks
                    createTask(
                        filename,
                        cacheListItem.position.start.line.toInt(),
                        fileContents[cacheListItem.position.start.line.toInt()]
                    )
                }
            } else {
                if (cacheListItem.task == null) {

                } else {

                }
            }
        }
    // Use mapNotNull() when collecting the subtasks/notes onto the parent?

    listItems
        .forEach { listItem ->
            val listItemLine = listItem.position.start.line.toInt()
            val lineContents = fileContents[listItemLine]
            // If the parent is negative (no parent set), or there is no task seen previously (so parent was not a task)
            if (listItem.parent.toInt() < 0 || !listItemsByLine.contains(listItem.parent.toInt())) {
                // Root level list item
                if (listItem.task != null) {
                    // Only care about root items that are tasks
                    listItemsByLine[listItemLine] = createTask(filename, listItemLine, lineContents)
                }
            } else {
                // Child list item
                val parentListItem = listItemsByLine[listItem.parent.toInt()]!! // TODO Handle error better
                if (listItem.task == null) {
                    // Is a note, find the parent task and add this line to the notes list
                    // removing the first two characters (the list marker, '- ')
                    val note = Note(lineContents.trim().drop(2), FilePosition(listItemLine))
                    listItemsByLine[listItemLine] = note
                    when (parentListItem) {
                        is Task -> listItemsByLine[listItem.parent.toInt()] = parentListItem.copy(notes = parentListItem.notes.plus(note))
                        is Note -> listItemsByLine[listItem.parent.toInt()] = parentListItem.copy(subnotes = parentListItem.subnotes.plus(note))
                    }

                } else {
                    // Is a task, construct task and find the parent task to add to subtasks list
                    val subtask = createTask(filename, listItemLine, lineContents)
                    when (parentListItem) {
                        is Task -> listItemsByLine[listItem.parent.toInt()] = parentListItem.copy(subtasks = parentListItem.subtasks.plus(subtask))
                        is Note -> {
                            console.log(" - ERROR: Trying to add Subtask to Note", parentListItem, subtask, listItem.parent)
                            throw IllegalStateException("Cannot add Subtask to Note")
                        }
                    }
                }
            }
        }

    return listItemsByLine.values.filterIsInstance<Task>()}

suspend fun writeModifiedTasks(tasks: List<Task>, vault: Vault) {

}

fun writeFile(vault: Vault, existingContents: String, tasks: List<Task>, file: TFile) {

}
