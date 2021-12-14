package service

import ListItemCache
import RepeatItem
import Task
import kotlin.js.Date
import moment.moment

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class TaskService() {
    // momentDateFormat = "yyyy-MM-DD"

    private val taskPaperTagDateFormat = """\(([0-9\-T:]*)\)"""
    private val dueDateRegex = Regex("""@due$taskPaperTagDateFormat""")
    private val completedDateRegex = Regex("""@completed$taskPaperTagDateFormat""")
    @Suppress("RegExpRedundantEscape")
    private val dataviewRegex = Regex("""\[([a-zA-Z]*):: ([\d\w!: -]*)\]""")
    private val spanValues = listOf("daily", "weekly", "monthly", "yearly")
    private val specificValues = listOf("month", "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val spanRegex = spanValues.plus(specificValues).joinToString("|")
    private val repeatItemRegex = Regex("""($spanRegex)([!]?): ([0-9]{1,2})""")
    private val allTagsRegex = Regex("""#([a-zA-Z][0-9a-zA-Z-_/]*)""")
    @Suppress("RegExpRedundantEscape")
    private val completedRegex = Regex("""- \[[xX]\]""")

    /**
     * Builds up a model of Tasks based on the file contents.
     *
     * Parameters are used instead of the TFile directly to simplify Promise usage, since vault.read()
     * return a Promise and this method returns an already built model. This method can now be called
     * within a Promise.then block with no issues.
     *
     * @param fileContents The contents of the file with each item being a line (file is split on newline)
     * @param listItems List of ListItemCache objects representing the list items in the file
     *
     * TODO Support 'root' tasks that are children of regular list items
     * TODO Support more than one child level (subtask of a subtask)
     */
    @Suppress("NON_EXPORTABLE_TYPE")
    fun buildTaskModel(fileContents: List<String>, listItems: Array<ListItemCache>) : Map<Int,Task> {
        val taskList = mutableMapOf<Int,Task>() // Map of position -> Task

        listItems.forEach { listItem ->
            val taskLine = listItem.position.start.line.toInt()
            val lineContents = fileContents[taskLine]
            if (listItem.parent.toInt() < 0) {
                // Root level list item
                if (listItem.task != null) {
                    // Only care about root items that are tasks
                    val task = createTask(lineContents, taskLine)
                    console.log("root task:", task)
                    taskList[listItem.position.start.line.toInt()] = task
                }
            } else {
                val parentTask = taskList[listItem.parent.toInt()]!! // TODO Handle error better
                // Child list item
                if (listItem.task == null) {
                    // Is a note, find the parent task and add this line to the notes list
                    // removing the first two characters (the list marker, '- ')
                    parentTask.notes.add(lineContents.trim().drop(2))
                } else {
                    // Is a task, construct task and find the parent task to add to subtasks list
                    val subtask = createTask(lineContents, taskLine)
                    console.log("Subtask for task at ${listItem.parent}: ", subtask)
                    parentTask.subtasks.add(subtask)
                }
            }
        }

        return taskList
    }


    fun createTask(text: String, line: Int? = null) : Task {
        // Pull out due and completed dates
        val due = getDueDateFromTask(text)
        val completedDate = getCompletedDateFromTask(text)

        // Pull out all tags
        val tagMatches = allTagsRegex.findAll(text).map { it.groupValues[1] }.toList()

        // Pull out all Dataview fields
        val dataviewMatches = mutableMapOf<String,String>()
        dataviewRegex.findAll(text).associateTo(dataviewMatches) {
            it.groupValues[1] to it.groupValues[2]
        }

         // TODO Replace with RegEx so listItem isn't required
        val completed = completedRegex.containsMatchIn(text)

        // Strip out due, tags, dataview and task notation from the text, then clean up whitespace
        val stripped = text
            .replace(dueDateRegex, "")
            .replace(allTagsRegex, "")
            .replace(dataviewRegex, "")
            .trim()
            .replace("""\s+""".toRegex(), " ")
            .replace("""- \[[Xx ]\] """.toRegex(), "")
        return Task(text, line, stripped, due, completedDate, tagMatches, dataviewMatches, completed)
    }

    /**
     * Detects whether the given task is a repeating task
     */
    fun isTaskRepeating(task: Task) : Boolean {
        return task.dataviewFields.containsKey("repeat") && task.due != null
    }

    /**
     * Gets the repeated task to the given task. Will do the following:
     * 1. Remove the @completed tag that CardBoard adds when a task is completed
     * 2. Marks as incomplete
     * 3. Replaces the due date with the next date in the cycle
     */
    fun getNextRepeatingTask(task: Task) : Task {
        val repeatTask = task.deepCopy()
        repeatTask.due = getNextRepeatDate(task)
        repeatTask.completed = false
        repeatTask.completedDate = null
        return repeatTask
    }

    fun removeRepeatText(task: Task) : Task {
        task.dataviewFields.remove("repeat")
        return task
    }

    private fun getNextRepeatDate(task: Task) : Date {
        // If there isn't a due date and repeating note then there is no next date
        if (!isTaskRepeating(task))
            throw IllegalArgumentException("Task requires a due date and repeating note to calculate next date\n\t$task")

        val repeatItem = parseRepeating(task.dataviewFields["repeat"]!!)
        val fromDate = if (repeatItem.fromComplete) Date() else task.due!!
        val currentYear = fromDate.getFullYear()
        val currentMonth = fromDate.getMonth()
        val currentDay = fromDate.getDate()

        return when (repeatItem.type) {
            "SPAN" ->
                when (repeatItem.span) {
                    "daily" -> Date(currentYear, currentMonth, currentDay + repeatItem.amount, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                    "weekly" -> Date(currentYear, currentMonth, currentDay + (repeatItem.amount * 7), fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                    "monthly" -> Date(currentYear, currentMonth + repeatItem.amount, currentDay, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                    "yearly" -> Date(currentYear + repeatItem.amount, currentMonth, currentDay, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                    else -> throw IllegalStateException("Recur span ${repeatItem.span} is not valid")
                }

            "SPECIFIC" ->
                when (repeatItem.span) {
                    "month" -> Date(currentYear, currentMonth + 1, repeatItem.amount, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                    else -> Date(currentYear + 1, currentMonth, currentDay, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                }
            else -> throw IllegalStateException("Recurring type ${repeatItem.type} is not valid")
        }
    }

    /**
     * Parse out the repeating text into a RepeatItem object
     */
    private fun parseRepeating(repeatText: String) : RepeatItem {
        console.log("repeatItemRegex: for repeatText", repeatItemRegex, repeatText)
        val matches = repeatItemRegex.find(repeatText)
        console.log("matches: ", matches)
        if (matches?.groupValues == null) {
            return RepeatItem("ERROR", "", false, 0)
        }

        var index = 1
        val type = matches.groupValues[index++]
        val fromComplete = matches.groupValues[index++] == "!"
        val amount = matches.groupValues[index].toInt()
        val repeatType = if (type in spanValues) "SPAN" else "SPECIFIC"

        return RepeatItem(repeatType, type, fromComplete, amount)
    }

    /**
     * Gets the due date from the task string
     *
     * So, given `Task @due(2021-01-01)`, this will return a Date object set to`2021-01-01`
     * TODO Support Dataview style inline fields
     *
     * @param task The task String
     * @return A Date object representing the due date or null if no due date is present
     */
    private fun getDueDateFromTask(task: String) : Date? {
        val dateMatch = dueDateRegex.find(task)
        console.log("dateMatch: ${dateMatch?.groupValues?.get(1)}")
        return if (dateMatch == null) {
            null
        } else {
            Date(dateMatch?.groupValues[1])
        }
    }

    /**
     * Gets the completed date from the task string
     *
     * So, given `Task @completed(2021-01-01)`, this will return a Date object set to`2021-01-01`
     * TODO Support Dataview style inline fields
     *
     * @param task The task String
     * @return A Date object representing the completed date or null if no completed date is present
     */
    private fun getCompletedDateFromTask(task: String) : Date? {
        val dateMatch = completedDateRegex.find(task)
        console.log("dateMatch: ${dateMatch?.groupValues?.get(1)}")
        return if (dateMatch == null) {
            null
        } else {
            Date(dateMatch?.groupValues[1])
        }
    }
}
