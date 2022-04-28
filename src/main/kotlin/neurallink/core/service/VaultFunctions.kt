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

    // Need to use recursion here to build up the Task list
    listItems
        .map { cacheListItem ->
            if (cacheListItem.task == null) {
                NoteInProcess(
                    noteFromLine(fileContents, cacheListItem),
                    cacheItemLine(cacheListItem),
                    cacheItemParent(cacheListItem)
                )
            } else {
                TaskInProcess(
                    taskFromLine(filename, fileContents, cacheListItem),
                    cacheItemLine(cacheListItem),
                    cacheItemParent(cacheListItem)
                )
            }
        }
        .sortedBy { it.line }
        .groupBy { it.parent }
        .map {  }

    listItems
        .map { cacheListItem ->
            if (cacheListItem.parent.toInt() < 0 || !listItemsByLine.contains(cacheItemParent(cacheListItem))) {
                // Root level list item
                if (cacheListItem.task != null) {
                    // Only care about root items that are tasks
                    createTask(
                        filename,
                        cacheItemLine(cacheListItem),
                        fileContents[cacheItemLine(cacheListItem)]
                    )
                }
            } else {
                // Child list item
                val parentListItem = listItemsByLine[cacheItemParent(cacheListItem)]!! // TODO Handle error better
                if (cacheListItem.task == null) {
                    // Is a note, find the parent task and add this line to the notes list
                    // removing the first two characters (the list marker, '- ')
                    val note = noteFromLine(fileContents, cacheListItem)
                    listItemsByLine[cacheItemLine(cacheListItem)] = note
                    when (parentListItem) {
                        is Task -> listItemsByLine[cacheItemParent(cacheListItem)] = parentListItem.copy(notes = parentListItem.notes.plus(note))
                        is Note -> listItemsByLine[cacheItemParent(cacheListItem)] = parentListItem.copy(subnotes = parentListItem.subnotes.plus(note))
                    }
                } else {
                    // Is a task, construct task and find the parent task to add to subtasks list
                    val subtask = createTask(filename, cacheItemLine(cacheListItem), lineContents(fileContents, cacheListItem))
                    when (parentListItem) {
                        is Task -> listItemsByLine[cacheItemParent(cacheListItem)] = parentListItem.copy(subtasks = parentListItem.subtasks.plus(subtask))
                        is Note -> {
                            console.log(" - ERROR: Trying to add Subtask to Note", parentListItem, subtask, cacheItemParent(cacheListItem))
                            throw IllegalStateException("Cannot add Subtask to Note")
                        }
                    }
                }
            }
        }
    // Use mapNotNull() when collecting the subtasks/notes onto the parent?

    return listItemsByLine.values.filterIsInstance<Task>()
}

fun buildTTaskTree(items: List<ItemInProcess>, parentId: Int) : List<ItemInProcess> {
    return items
        .filter { itemInProcess -> itemInProcess.parent == parentId }
}

fun lineContents(fileContents: List<String>, item: ListItemCache) : String {
    return fileContents[cacheItemLine(item)].trim().drop(2)
}

fun noteFromLine(fileContents: List<String>, item: ListItemCache) : Note {
    return Note(
        lineContents(fileContents, item),
        FilePosition(cacheItemLine(item))
    )
}

fun taskFromLine(filename: String, fileContents: List<String>, item: ListItemCache) : Task {
    return createTask(filename, cacheItemLine(item), fileContents[cacheItemLine(item)])
}

fun cacheItemLine(item: ListItemCache) : Int {
    return item.position.start.line.toInt()
}

fun cacheItemParent(item: ListItemCache) : Int {
    return item.parent.toInt()
}

suspend fun writeModifiedTasks(tasks: List<Task>, vault: Vault) {

}

fun writeFile(vault: Vault, existingContents: String, tasks: List<Task>, file: TFile) {

}

sealed class ItemInProcess(open val line: Int, open val parent: Int)
data class TaskInProcess(val task: Task, override val line: Int, override val parent: Int) : ItemInProcess(line, parent)
data class NoteInProcess(val note: Note, override val line: Int, override val parent: Int) : ItemInProcess(line, parent)