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
    // Need to use recursion here to build up the Task list
    return listItems
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
        .convertToTree()
}

/**
 * Simple extension function to convert a list of ItemInProcess objects to a list of Tasks
 * with all subtasks and notes in place.
 *
 * @return A list of root level Tasks
 */
fun List<ItemInProcess>.convertToTree() : List<Task> {
    return buildRootTaskTree(this)
}

fun buildRootTaskTree(items: List<ItemInProcess>) : List<Task> {
    return items
        // FIXME This won't handle the case of a task under a note
        .filter { item -> item.parent < 0 }
        .mapNotNull { item ->
            if (item is TaskInProcess) {
                item.task.copy(
                    subtasks = buildTTaskTree(items, item.line),
                    notes = buildNoteTree(items, item.line)
                )
            } else {
                null
            }
        }
}

fun buildTTaskTree(items: List<ItemInProcess>, parentId: Int) : List<Task> {
    return items
        .filter { item -> item is TaskInProcess && item.parent == parentId }
        .mapNotNull { item ->
            if (item is TaskInProcess) {
                item.task.copy(
                    subtasks = buildTTaskTree(items, item.line),
                    notes = buildNoteTree(items, item.line)
                )
            } else {
                null
            }
        }
}

fun buildNoteTree(items: List<ItemInProcess>, parentId: Int) : List<Note> {
    println("buildNoteTree(): $parentId, $items")
    return items
        .filter { item -> item is NoteInProcess && item.parent == parentId }
        .mapNotNull { item ->
            if (item is NoteInProcess) {
                item.note.copy(
                    subnotes = buildNoteTree(items, item.line)
                )
            } else {
                null
            }
        }
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
