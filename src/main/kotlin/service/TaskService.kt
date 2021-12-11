package service

import ListItemCache
import Task
import kotlin.js.Date
import moment.moment

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class TaskService() {
    private val dueDateFormat = "yyyy-MM-DD"

    private val taskPaperTagDateFormat = """\(([0-9\-T:]*)\)"""
    private val dueDateRegex = Regex("""@due$taskPaperTagDateFormat""")
    private val completedDateRegex = Regex("""@completed$taskPaperTagDateFormat""")
    private val repeatingRequires = listOf("@due(", "[repeat::")
    @Suppress("RegExpRedundantEscape")
    private val dataviewRegex = Regex("""\[([a-zA-Z]*):: ([\d\w!: -]*)\]""")
    @Suppress("RegExpRedundantEscape")
    private val repeatTextRegex = Regex("""\[repeat:: ([\d\w!: ]*)\]""")

    private val spanValues = listOf("daily", "weekly", "monthly", "yearly")
    private val specificValues = listOf("month", "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val spanRegex = spanValues.plus(specificValues).joinToString("|")
    private val repeatItemRegex = Regex("""($spanRegex)([!]?): ([0-9]{1,2})""")
    @Suppress("RegExpRedundantEscape")
    private val completedTaskRegex = Regex("""- \[[xX]\] """)
    private val allTagsRegex = Regex("""#([a-zA-Z][0-9a-zA-Z-_/]*)""")

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
            val lineContents = fileContents[listItem.position.start.line.toInt()]
            if (listItem.parent.toInt() < 0) {
                // Root level list item
                if (listItem.task != null) {
                    // Only care about root items that are tasks
                    val task = createTask(listItem, lineContents)
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
                    val subtask = createTask(listItem, lineContents)
                    console.log("Subtask for task at ${listItem.parent}: ", subtask)
                    parentTask.subtasks.add(subtask)
                }
            }
        }

        return taskList
    }

    private fun createTask(listItem: ListItemCache, text: String) : Task {
        // Pull out due date
        val due = if (text.contains("@due(")) getDueDateFromTask(text) else null

        // Pull out all tags
        val tagMatches = allTagsRegex.findAll(text).map { it.groupValues[1] }.toList()

        // Pull out all Dataview fields
        val dataviewMatches = dataviewRegex.findAll(text).associate {
            it.groupValues[1] to it.groupValues[2]
        }

        val completed = (listItem.task?.uppercase() == "X")

        // Strip out due, tags, dataview and task notation from the text, then clean up whitespace
        val stripped = text
            .replace(dueDateRegex, "")
            .replace(allTagsRegex, "")
            .replace(dataviewRegex, "")
            .trim()
            .replace("""\s+""".toRegex(), " ")
            .replace("""- \[[Xx ]\] """.toRegex(), "")
        return Task(text, stripped, due, tagMatches, dataviewMatches, completed)
    }

    /**
     * Detects whether the given task is a repeating task
     */
    fun isTaskRepeating(task: String) : Boolean {
        return repeatingRequires.all { task.contains(it) }
    }

    /**
     * Gets the repeated task to the given task. Will do the following:
     * 1. Remove the @completed tag that CardBoard adds when a task is completed
     * 2. Unchecks the checkbox
     * 3. Replaces the due date with the next date in the cycle
     */
    fun getNextRepeatingTask(task: String) : String {
        val nextDate = getNextRepeatDate(task)
        console.log("getNextRepeatingTask for: ", task)
        var nextTaskVersion = task.replace(completedTaskRegex, "- [ ] ")
        console.log("\tafter completedTaskRegex: ", nextTaskVersion)
        nextTaskVersion = task.replace(completedDateRegex, "")
        console.log("\tafter completedDateRegex: ", nextTaskVersion)
        nextTaskVersion = task.replace(dueDateRegex, "@due(${moment(nextDate).format(dueDateFormat)})")
        console.log("\tafter dueDateRegex: ", nextTaskVersion)

        return task
            .replace(completedTaskRegex, "- [ ] ")
            .replace(completedDateRegex, "")
            .replace(dueDateRegex, "@due(${moment(nextDate).format(dueDateFormat)})")
    }

    fun removeRepeatText(task: String) : String {
        return task.replace(repeatTextRegex, "")
    }

    private fun getNextRepeatDate(task: String) : Date {
        // If there isn't a due date and repeating note then there is no next date
        if (!isTaskRepeating(task))
            throw IllegalArgumentException("Task requires a due date and repeating note to calculate next date\n\t$task")

        val repeatMatches = repeatTextRegex.find(task) ?: throw IllegalStateException("No match found for repeating note")
        console.log("repeatMatches: ", repeatMatches)
        val repeatItem = parseRepeating(repeatMatches.groupValues[1])

        val fromDate = if (repeatItem.fromComplete) Date() else getDueDateFromTask(task)
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
     * Gets a date from the task string tagged with the given tag.
     *
     * So, given `Task @due(2021-01-01T00:00:00)`, this will return a Date object set to`2021-01-01T00:00:00` when asking for the `due` tag
     *
     * TODO Support Dataview style inline fields
     *
     * @param task The task String
     * @return A Date object representing the due date or null if no due date is present
     */
    private fun getDueDateFromTask(task: String) : Date {
        val dateMatch = dueDateRegex.find(task)
        console.log("dateMatch: ${dateMatch?.groupValues?.get(1)}")
        return dateMatch?.let {
            return Date(dateMatch.groupValues[1])
        } ?: throw IllegalStateException("No match found for due date in task text")
    }
}

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
data class RepeatItem(val type: String, val span: String, val fromComplete: Boolean, val amount: Int)

data class ModifiedTask(var original: String, val before: MutableList<String>, val after: MutableList<String>) {
    constructor(original: String) : this(original, mutableListOf(), mutableListOf())
}
