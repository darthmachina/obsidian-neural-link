package neurallink.core.service

import ListItemCache
import MetadataCache
import TFile
import Vault
import arrow.core.Either
import arrow.core.flatMap
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import neurallink.core.model.NeuralLinkModel
import neurallink.core.model.*
import neurallink.core.store.VaultLoaded
import org.reduxkotlin.Store

private val logger = KotlinLogging.logger("VaultFunctions")

// ************* FILE READING *************
fun loadTasKModelIntoStore(vault: Vault, metadataCache: MetadataCache, store: Store<NeuralLinkModel>) {
    CoroutineScope(Dispatchers.Main).launch {
        store.dispatch(VaultLoaded(processAllFiles(store, vault, metadataCache)))
    }
}

suspend fun Array<TFile>.filterFiles(store: Store<NeuralLinkModel>) : List<TFile> {
    return this.filter {
            // Process only Markdown files
            it.name.endsWith(".md")
        }
        .filter { file ->
            // Ignore any files that are in the ignorePaths list
            !pathInPathList(file.path, store.state.settings.ignorePaths)
        }

}

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
suspend fun processAllFiles(store: Store<NeuralLinkModel>, vault: Vault, metadataCache: MetadataCache): List<Task>
        = coroutineScope {
    logger.debug { "processAllFiles()" }
    val fileSemaphore = Semaphore(2)

    vault.getFiles()
        .filterFiles(store)
        .filter { file ->
            // Only include files that have a task
            val listItems = metadataCache.getFileCache(file)?.listItems?.toList() ?: emptyList()
            listItems.any { listItemCache ->
                listItemCache.task != null
            }
        }
        .map { file ->
            // Read all files
            async {
                fileSemaphore.withPermit {
                    readFile(store, file, vault, metadataCache)
                }
            }
        }.awaitAll()
        .flatten()
}

suspend fun readFile(store: Store<NeuralLinkModel>, file: TFile, vault: Vault, metadataCache: MetadataCache): List<Task> {
    logger.debug { "readFile(): ${file.name}" }
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
        taskList.addAll(
            processFile(file.path, fileContents, fileListItems)
                .filter { task ->
                    !task.completed ||
                            task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)) ||
                            task.tags.any { tag -> tag in store.state.settings.columnTags.map { it.tag } }
                }
        )
    }.await()
    return taskList
}

fun processFile(
    filename: String,
    fileContents: List<String>,
    listItems: Array<ListItemCache>
): List<Task> {
    logger.debug { "processFile(): $filename" }
    // Need to use recursion here to build up the Task list
    return listItems
        .map { cacheListItem ->
            logger.trace { " - checking $cacheListItem" }
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
//    neurallink.core.settings.logger.debug { "buildTaskTree(): parentId: $parentId" }
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
//    neurallink.core.settings.logger.debug { "buildNoteTree(): $parentId" }
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

// ************* FILE WRITING *************

data class FileContentsWithTasks(
    val lineText: String,
    val task: Task?,
    val linesToRemove: List<Int>
)

suspend fun writeModifiedTasks(tasks: List<Task>, vault: Vault) {
    logger.debug { "writeModifiedTasks()" }
    withContext(CoroutineScope(Dispatchers.Main).coroutineContext) {
        tasks
            .filter { it.original != null }
            .map { it }
            .groupBy { it.file }
            .forEach { entry ->
                launch {
                    val file = vault.getAbstractFileByPath(entry.key.value) as TFile
                    vault.read(file).then { contents ->
                        writeFile(vault, contents, entry.value, file)
                    }
                }
            }
    }
}

fun writeFile(vault: Vault, existingContents: String, tasks: List<Task>, file: TFile) {
    logger.debug { "writeFile(): ${file.name}" }
    createFileContents(existingContents, tasks)
        .map { vault.modify(file, it) }
        .mapLeft {
            logger.debug { "File was not modified, not writing to disk" }
        }
}

fun createFileContents(existingContents: String, tasks: List<Task>) : Either<TaskWritingWarning,String> {
    return joinFileContentsWithTasks(existingContents.split('\n'), tasks)
        .mapValues {
            if (it.value.task != null) {
                Triple(toMarkdown(it.value.task!!), it.value.linesToRemove, true)
            } else {
                Triple(it.value.lineText, null, false)
            }
        }
        .markRemoveLines()
        .map {
            it.mapNotNull {
                if (it.second) {
                    null
                } else {
                    it.first
                }
            }
        }
        .flatMap {
            Either.Right(it.joinToString("\n"))
        }
}

/**
 * Converts a Map of file lines with a list of indented lines to remove into a
 * list of strings with a boolean for whether to remove the line.
 */
fun Map<Int,Triple<String,List<Int>?,Boolean>>.markRemoveLines() : Either<TaskWritingWarning, List<Pair<String, Boolean>>> {
    // Check for modifications and return Either.Left if the file was not modified
    if (this.values.none { it.third }) {
        return Either.Left(TaskWritingWarning("File was not modified"))
    }

    // Basically caching the value of the fold operation instead of running it every iteration
    // TODO Memoize this in a function, but need to find out how to memoize in Kotlin
    val removeList = this.values.fold(listOf<Int>()) { accu, item ->
        accu.plus(if (item.second == null) emptyList() else item.second!!)
    }

    return Either.Right(this
        .entries
        .map {
            Pair(it.value.first, removeList.contains(it.key))
        }
    )
}

/**
 * Joins file contents with the list of tasks, using the line number as a key
 *
 * @return A map of line number to a Triple containing: current file contents, task for line if it exists, list of lines to remove if a task exists on this line
 */
fun joinFileContentsWithTasks(existingContents: List<String>, tasks: List<Task>) : Map<Int,FileContentsWithTasks> {
    return existingContents
        .mapIndexed { index, line -> Pair(index, line) }
        .groupBy(
            keySelector = { it.first },
            valueTransform = {
                Pair(
                    it.second,
                    tasks.find { task ->
                        task.filePosition.value == it.first
                    }
                )
            }
        )
        .mapValues { it.value[0] } // Will only be one Pair as the key is the line in the file
        .mapValues { FileContentsWithTasks(
            it.value.first,
            it.value.second,
            if (it.value.second == null)
                emptyList()
            else
                expandRemovalLines(it.key, indentedCount(it.value.second!!))
        )}
}

/**
 * Recursive method to get the number of indented items.
 */
fun indentedCount(task: Task) : Int {
    return if (task.original == null || (task.original.subtasks.isEmpty() && task.original.notes.isEmpty())) {
        0
    } else {
        task.original.subtasks.size +
            task.original.notes.size +
            task.original.subtasks.fold(0) { accumulator, subtask ->
                accumulator + indentedSubtaskCount(subtask)
            } +
            task.original.notes.fold(0) { accumulator, note ->
                accumulator + indentedNoteCount(note)
            }
    }
}

/**
 * Gets all source files on all tasks.
 */
fun getAllSourceFiles(tasks: List<Task>) : List<String> {
    return tasks
        .map { task ->
            task.file.value
        }
        .distinct()
        .sorted()
        .map { it.dropLast(3) }
}

/**
 * Recursive method to get the number of indented subtasks.
 */
fun indentedSubtaskCount(task: Task) : Int {
    return if (task.subtasks.isEmpty() && task.notes.isEmpty()) {
        0
    } else {
        task.subtasks.size +
                task.notes.size +
                task.subtasks.fold(0) { accumulator, subtask ->
                    accumulator + indentedCount(subtask)
                } +
                task.notes.fold(0) { accumulator, note ->
                    accumulator + indentedNoteCount(note)
                }
    }
}

/**
 * Recursive method to get the number of indented Notes.
 */
fun indentedNoteCount(note: Note) : Int {
    return if (note.subnotes.isEmpty()) {
        0
    } else {
        note.subnotes.size +
            note.subnotes.fold(0) { accu, note ->
                accu + indentedNoteCount(note)
            }
    }
}

/**
 * Given a line number and an indented count, returns a list of lines to remove
 */
fun expandRemovalLines(line: Int, indentedCount: Int) : List<Int> {
    return ((line + 1)until(line + 1 + indentedCount)).map {
        it
    }
}

sealed class ItemInProcess(open val line: Int, open val parent: Int)
data class TaskInProcess(val task: Task, override val line: Int, override val parent: Int) : ItemInProcess(line, parent)
data class NoteInProcess(val note: Note, override val line: Int, override val parent: Int) : ItemInProcess(line, parent)
