package service

import ListItemCache
import MetadataCache
import TFile
import Vault
import kotlinx.coroutines.*
import model.SimpleDate
import model.Task
import model.TaskModel
import org.reduxkotlin.Store
import store.VaultLoaded

private const val TASK_PAPER_DATE_FORMAT = """\(([0-9\-T:]*)\)"""

/**
 * Service for interacting with a TaskModel. Main use is to process files
 * in the Vault to build up the list of tasks and to process modified/created
 * files to incorporate those changes into the vault.
 *
 * TODO: Create a method to process a modified/created file
 */
class TaskModelService {
    private val dueDateRegex = Regex("""@due$TASK_PAPER_DATE_FORMAT""")
    private val completedDateRegex = Regex("""@completed$TASK_PAPER_DATE_FORMAT""")
    @Suppress("RegExpRedundantEscape")
    private val dataviewRegex = Regex("""\[([a-zA-Z]*):: ([\d\w!: -]*)\]""")
    private val spanValues = listOf("daily", "weekly", "monthly", "yearly", "weekday")
    private val specificValues = listOf("month", "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val spanRegex = spanValues.plus(specificValues).joinToString("|")
    private val repeatItemRegex = Regex("""($spanRegex)([!]?)(: ([0-9]{1,2}))?""")
    private val allTagsRegex = Regex("""#([a-zA-Z][0-9a-zA-Z-_/]*)""")
    @Suppress("RegExpRedundantEscape")
    private val completedRegex = Regex("""- \[[xX]\]""")

    fun loadTasKModelIntoStore(vault: Vault, metadataCache: MetadataCache, store: Store<TaskModel>) {
        val taskModel = TaskModel(store.state.settings) // Reuse Setting from Store

        val jobList = listOf<Job>()
        val deferredJob = CoroutineScope(Dispatchers.Main).launch {
            val taskModel = processAllFiles(vault, metadataCache, taskModel)
            store.dispatch(VaultLoaded(taskModel))
        }
    }

    private suspend fun processAllFiles(vault: Vault, metadataCache: MetadataCache, taskModel: TaskModel): TaskModel = coroutineScope {
        vault.getFiles().map { file ->
            async {
                taskModel.tasks.addAll(readFile(file, vault, metadataCache))
            }
        }.awaitAll()

        taskModel
    }

    private suspend fun readFile(file: TFile, vault: Vault, metadataCache: MetadataCache): MutableList<Task> {
        val fullname = file.path + '/' + file.name
        val taskList = mutableListOf<Task>()
        vault.read(file).then { contents ->
            val fileContents = contents.split('\n')
            val fileListItems = metadataCache.getFileCache(file)?.listItems ?: arrayOf()
            val tasksForFile = processFile(fullname, fileContents, fileListItems)
            taskList.addAll(tasksForFile)
        }.await()
        return taskList
    }

    private fun processFile(
        filename: String,
        fileContents: List<String>,
        listItems: Array<ListItemCache>
    ): MutableList<Task> {
        val tasksByLine = mutableMapOf<Int,Task>() // Map of position -> Task

        listItems.forEach { listItem ->
            val taskLine = listItem.position.start.line.toInt()
            val lineContents = fileContents[taskLine]
            if (listItem.parent.toInt() < 0) {
                // Root level list item
                if (listItem.task != null) {
                    // Only care about root items that are tasks
                    val task = createTask(filename, taskLine, lineContents)
                    tasksByLine[listItem.position.start.line.toInt()] = task
                }
            } else {
                val parentTask = tasksByLine[listItem.parent.toInt()]!! // TODO Handle error better
                // Child list item
                if (listItem.task == null) {
                    // Is a note, find the parent task and add this line to the notes list
                    // removing the first two characters (the list marker, '- ')
                    parentTask.notes.add(lineContents.trim().drop(2))
                } else {
                    // Is a task, construct task and find the parent task to add to subtasks list
                    val subtask = createTask(filename, taskLine, lineContents)
                    parentTask.subtasks.add(subtask)
                }
            }
        }

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
                timeSplit[2].toInt()  // Second
            )
        }
    }
    /**
     * Recursive method to get the number of indented items.
     */
    fun indentedCount(task: Task) : Int {
        return if (task.subtasks.size == 0 && task.notes.size == 0) {
            0
        } else {
            task.subtasks.size + task.notes.size + task.subtasks.fold(0) { accumulator, subtask ->
                accumulator + indentedCount(subtask)
            }
        }
    }
}