package service

import NeuralLinkPlugin
import kotlin.js.Date
import moment.moment

@OptIn(ExperimentalJsExport::class)
@JsExport
class TaskService(plugin: NeuralLinkPlugin) {
    private val dueDateFormat = "yyyy-MM-DDTHH:mm:ss"

    private val dueDateRegex = Regex("""@due\(([0-9\-T:]*)\)""")
//    private val isRecurringTaskRegex = Regex("""((@due.*\[recur::)|(\[recur::.*@due))""")
    private val recurringRequires = listOf("@due(", "[recur::")
    @Suppress("RegExpRedundantEscape")
    private val recurTextRegex = Regex("""\[recur:: ([\d\w: ]*)\]""")

    private val spanValues = listOf("daily", "weekly", "monthly", "yearly")
    private val specificValues = listOf("month", "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val spanRegex = spanValues.plus(specificValues).joinToString("|")
    private val recurItemRegex = Regex("""($spanRegex)([!]?): ([0-9]{1,2})""")
    private val completedTaskRegex = Regex("""- \[[xX]\] """)

    fun isTaskRecurring(task: String) : Boolean {
        return recurringRequires.all { task.contains(it) }
    }

    fun getNextRecurringTask(task: String) : String {
        val nextDate = getNextRecurDate(task)
        return task
            .replace(completedTaskRegex, "- [ ] ")
            .replace(dueDateRegex, "@due(${moment(nextDate).format(dueDateFormat)})")
    }

    fun removeRecurText(task: String) : String {
        return task.replace(recurTextRegex, "")
    }

    private fun getNextRecurDate(task: String) : Date {
        // If there isn't a due date and recurring note then there is no next date
        if (!isTaskRecurring(task))
            throw IllegalArgumentException("Task requires a due date and recurring note to calculate next date\n\t$task")


        val recurMatches = recurTextRegex.find(task) ?: throw IllegalStateException("No match found for recurring note")
        console.log("recurMatches: ", recurMatches)
        val recurItem = parseRecurring(recurMatches.groupValues[1])

        val fromDate = if (recurItem.fromComplete) Date() else getDueDateFromTask(task)
        val currentYear = fromDate.getFullYear()
        val currentMonth = fromDate.getMonth()
        val currentDay = fromDate.getDate()

        return when (recurItem.type) {
            "SPAN" ->
                when (recurItem.span) {
                    "daily" -> Date(currentYear, currentMonth, currentDay + recurItem.amount, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                    "weekly" -> Date(currentYear, currentMonth, currentDay + (recurItem.amount * 7), fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                    "monthly" -> Date(currentYear, currentMonth + recurItem.amount, currentDay, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                    "yearly" -> Date(currentYear + recurItem.amount, currentMonth, currentDay, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                    else -> throw IllegalStateException("Recur span ${recurItem.span} is not valid")
                }

            "SPECIFIC" ->
                when (recurItem.span) {
                    "month" -> Date(currentYear, currentMonth + 1, recurItem.amount, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                    else -> Date(currentYear + 1, currentMonth, currentDay, fromDate.getHours(), fromDate.getMinutes(), fromDate.getSeconds())
                }
            else -> throw IllegalStateException("Recurring type ${recurItem.type} is not valid")
        }
    }

    /**
     * Parse out the recurring text into a RecurItem object
     */
    private fun parseRecurring(recurText: String) : RecurItem {
        console.log("recurItemRegex: for recurText", recurItemRegex, recurText)
        val matches = recurItemRegex.find(recurText)
        console.log("matches: ", matches)
        if (matches?.groupValues == null) {
            return RecurItem("ERROR", "", false, 0)
        }

        var index = 1
        val type = matches.groupValues[index++]
        val fromComplete = matches.groupValues[index++] == "!"
        val amount = matches.groupValues[index].toInt()
        val recurType = if (type in spanValues) "SPAN" else "SPECIFIC"

        return RecurItem(recurType, type, fromComplete, amount)
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

@OptIn(ExperimentalJsExport::class)
@JsExport
data class RecurItem(val type: String, val span: String, val fromComplete: Boolean, val amount: Int)
