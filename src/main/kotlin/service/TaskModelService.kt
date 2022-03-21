package service

import ListItemCache
import MetadataCache
import TFile
import Vault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.SimpleDate
import model.Task
import model.TaskConstants
import model.TaskModel
import org.reduxkotlin.Store
import store.VaultLoaded

private const val TASK_PAPER_DATE_FORMAT = """\(([0-9\-T:]*)\)"""
private const val ALL_TAGS_REGEX = """#([a-zA-Z][0-9a-zA-Z-_/]*)"""
private const val DATAVIEW_REGEX = """\[([a-zA-Z]*):: ([\d\w!: -]*)\]"""
@Suppress("RegExpRedundantEscape")
private const val COMPLETED_REGEX = """- \[[xX]\]"""

/**
 * Service for interacting with a TaskModel. Main use is to process files
 * in the Vault to build up the list of tasks and to process modified/created
 * files to incorporate those changes into the vault.
 *
 * TODO: Create a method to process a modified/created file
 */
class TaskModelService {
    private val dueDateRegex = Regex("""@${TaskConstants.DUE_ON_PROPERTY}$TASK_PAPER_DATE_FORMAT""")
    private val completedDateRegex = Regex("""@${TaskConstants.COMPLETED_ON_PROPERTY}$TASK_PAPER_DATE_FORMAT""")
    @Suppress("RegExpRedundantEscape")
    private val dataviewRegex = Regex(DATAVIEW_REGEX)
    private val spanRegex =
        TaskConstants.REPEATING_TYPE.getAllTags()
            .plus(TaskConstants.SPECIFIC_INSTANTS.getAllTags())
            .joinToString("|")
    private val repeatItemRegex = Regex("""($spanRegex)([!]?)(: ([0-9]{1,2}))?""")
    private val allTagsRegex = Regex(ALL_TAGS_REGEX)
    private val completedRegex = Regex(COMPLETED_REGEX)

    fun loadTasKModelIntoStore(vault: Vault, metadataCache: MetadataCache, store: Store<TaskModel>) {
        CoroutineScope(Dispatchers.Main).launch {
            store.dispatch(VaultLoaded(processAllFiles(vault, metadataCache, store.state.copy())))
        }
    }

    suspend fun writeModifiedTasks(tasks: List<Task>, vault: Vault) {
        console.log("writeModifiedTasks()", tasks)
        withContext(CoroutineScope(Dispatchers.Main).coroutineContext) {
            tasks
                .filter { it.original != null }
                .groupBy { it.file }
                .forEach { entry ->
                launch {
                    console.log(" - writing file ${entry.key}")
                    val file = vault.getAbstractFileByPath(entry.key) as TFile
                    val linesToRemove = mutableListOf<Int>()
                    vault.read(file).then { contents ->
                        val fileContents = mutableListOf<String>().apply {
                            addAll(contents.split('\n'))
                        }
                        entry.value
                            .sortedByDescending { it.filePosition }
                            .forEach { task ->
                                console.log(" - Updating task : ${task.description}")
                                fileContents[task.filePosition] = task.toMarkdown()
                                val indentedCount = indentedCount(task.original!!)
                                if (indentedCount > 0) {
                                    val firstIndent = task.filePosition + 1
                                    // Use 'until' as we don't include the last element (indentedCount includes the firstIndent line)
                                    linesToRemove.addAll((firstIndent until (firstIndent + indentedCount)).toList())
                                    console.log(" - linesToRemove now", linesToRemove)
                                }
                            }
                        linesToRemove.sortedDescending().forEach {
                            fileContents.removeAt(it)
                        }
                        vault.modify(file, fileContents.joinToString("\n"))

                        // TODO Do I need to unset task.original? Or will that happen when I re-read the file
                    }
                }
            }
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
     * @param taskModel The TaskModel to populate
     * @return A filled TaskModel. This is the same instance as the taskModel parameter
     */
    private suspend fun processAllFiles(vault: Vault, metadataCache: MetadataCache, taskModel: TaskModel): TaskModel
    = coroutineScope {
        vault.getFiles().map { file ->
            async {
                taskModel.tasks.addAll(readFile(file, vault, metadataCache))
            }
        }.awaitAll()

        taskModel
    }

    suspend fun readFile(file: TFile, vault: Vault, metadataCache: MetadataCache): MutableList<Task> {
        val taskList = mutableListOf<Task>()
        vault.read(file).then { contents ->
            val fileContents = contents.split('\n')
            val fileListItems = metadataCache.getFileCache(file)?.listItems ?: arrayOf()
            val tasksForFile = processFile(file.path, fileContents, fileListItems)
            taskList.addAll(tasksForFile)
        }.await()
        return taskList
    }

    private fun processFile(
        filename: String,
        fileContents: List<String>,
        listItems: Array<ListItemCache>
    ): MutableList<Task> {
        console.log("processFile()", filename)
        val tasksByLine = mutableMapOf<Int,Task>() // Map of position -> Task

        listItems.forEach { listItem ->
            console.log(" - loading listItem", listItem)
            val taskLine = listItem.position.start.line.toInt()
            val lineContents = fileContents[taskLine]
            if (listItem.parent.toInt() < 0) {
                console.log(" - is a root level item")
                // Root level list item
                if (listItem.task != null) {
                    console.log(" - is a task so add it")
                    // Only care about root items that are tasks
                    val task = createTask(filename, taskLine, lineContents)
                    tasksByLine[listItem.position.start.line.toInt()] = task
                }
            } else {
                console.log(" - is an indented item")
                val parentTask = tasksByLine[listItem.parent.toInt()]!! // TODO Handle error better
                // Child list item
                if (listItem.task == null) {
                    console.log(" - is a note")
                    // Is a note, find the parent task and add this line to the notes list
                    // removing the first two characters (the list marker, '- ')
                    parentTask.notes.add(lineContents.trim().drop(2))
                } else {
                    console.log(" - is a subtask")
                    // Is a task, construct task and find the parent task to add to subtasks list
                    val subtask = createTask(filename, taskLine, lineContents)
                    parentTask.subtasks.add(subtask)
                }
            }
        }

        console.log("processFile() end")
        return tasksByLine.values.toMutableList()
    }

    private fun createTask(file: String, line: Int, text: String) : Task {
        // Pull out due and completed dates
        val due = getDueDateFromTask(text)
        val completedDate = getCompletedDateFromTask(text)

        // Pull out all tags
        val tagMatches = allTagsRegex.findAll(text).map { it.groupValues[1] }.toMutableSet()

        // Pull out all Dataview fields
        val dataviewMatches = mutableMapOf<String,String>()
        dataviewRegex.findAll(text).associateTo(dataviewMatches) {
            it.groupValues[1] to it.groupValues[2]
        }

        val completed = completedRegex.containsMatchIn(text)

        // Strip out due, tags, dataview and task notation from the text, then clean up whitespace
        @Suppress("RegExpRedundantEscape")
        val stripped = text
            .replace(dueDateRegex, "")
            .replace(completedDateRegex, "")
            .replace(allTagsRegex, "")
            .replace(dataviewRegex, "")
            .trim()
            .replace("""\s+""".toRegex(), " ")
            .replace("""- \[[Xx ]\] """.toRegex(), "")
        return Task(
            file,
            line,
            stripped,
            due,
            completedDate,
            tagMatches,
            dataviewMatches,
            completed
        )
    }

    /**
     * Gets the due date from the task string
     *
     * So, given `Task @due(2021-01-01)`, this will return a Date object set to`2021-01-01`
     * TODO Support Dataview style inline field for Due Date
     *
     * @param task The task String
     * @return A SimpleDate object representing the due date or null if no due date is present
     */
    private fun getDueDateFromTask(task: String) : SimpleDate? {
        val dateMatch = dueDateRegex.find(task)
        return if (dateMatch == null) {
            null
        } else {
            val dateSplit = dateMatch.groupValues[1].split('-')
            SimpleDate(
                dateSplit[0].toInt(),
                dateSplit[1].toInt(),
                dateSplit[2].toInt()
            )
        }
    }


    /**
     * Gets the completed date from the task string
     *
     * So, given `Task @completed(2021-01-01)`, this will return a Date object set to`2021-01-01`
     * TODO Support Dataview style inline field for Completed Date
     *
     * @param task The task String
     * @return A Date object representing the completed date or null if no completed date is present
     */
    private fun getCompletedDateFromTask(task: String) : SimpleDate? {
        val dateMatch = completedDateRegex.find(task)
        return if (dateMatch == null) {
            null
        } else {
            val dateAndTime = dateMatch.groupValues[1].split("T")
            val dateSplit = dateAndTime[0].split('-')
            val timeSplit = dateAndTime[1].split(':')
            SimpleDate(
                dateSplit[0].toInt(), // Year
                dateSplit[1].toInt(), // Month
                dateSplit[2].toInt(), // Day
                timeSplit[0].toInt(), // Hour
                timeSplit[1].toInt(), // Minute
                timeSplit[2].toInt(), // Second
                includeTime = true
            )
        }
    }
    /**
     * Recursive method to get the number of indented items.
     *
     * TODO: Does not handle notes with subnotes
     */
    private fun indentedCount(task: Task) : Int {
        return if (task.subtasks.size == 0 && task.notes.size == 0) {
            0
        } else {
            task.subtasks.size +
                    task.notes.size +
                    task.subtasks.fold(0) { accumulator, subtask ->
                        accumulator + indentedCount(subtask)
                    }

        }
    }
}
