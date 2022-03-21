package service

import model.RepeatItem
import model.SimpleDate
import model.Task
import model.TaskConstants
import kotlin.js.Date

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class RepeatingTaskService {
    /**
     * Detects whether the given task is a repeating task.
     */
    fun isTaskRepeating(task: Task) : Boolean {
        return task.dataviewFields.containsKey("repeat") && task.dueOn != null
    }

    /**
     * Gets the repeated task to the given task. Will do the following:
     * 1. Remove the @completed tag that CardBoard adds when a task is completed
     * 2. Marks as incomplete
     * 3. Replaces the due date with the next date in the cycle
     */
    fun getNextRepeatingTask(task: Task) : Task {
        console.log("getNextRepeatingTask()", task)
        val repeatTask = task.deepCopy()
        repeatTask.dueOn = getNextRepeatDate(task)
        repeatTask.completed = false
        repeatTask.completedOn = null
        return repeatTask
    }

    private fun getNextRepeatDate(task: Task) : SimpleDate {
        // If there isn't a due date and repeating note then there is no next date
        if (!isTaskRepeating(task))
            throw IllegalArgumentException("Task requires a due date and repeating note to calculate next date\n\t$task")

        val repeatItem = parseRepeating(task.dataviewFields["repeat"]!!)
        val currentDate = Date()
        val fromDate = if (repeatItem.fromComplete) SimpleDate(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate()) else task.dueOn!!

        return when (repeatItem.span) {
            TaskConstants.REPEAT_SPAN.DAILY -> SimpleDate(fromDate.year, fromDate.month, fromDate.day + repeatItem.amount)
            TaskConstants.REPEAT_SPAN.WEEKLY -> SimpleDate(fromDate.year, fromDate.month, fromDate.day + (repeatItem.amount * 7))
            TaskConstants.REPEAT_SPAN.MONTHLY -> SimpleDate(fromDate.year, fromDate.month + repeatItem.amount, fromDate.day)
            TaskConstants.REPEAT_SPAN.YEARLY -> SimpleDate(fromDate.year + repeatItem.amount, fromDate.month, fromDate.day)
            TaskConstants.REPEAT_SPAN.WEEKDAY -> {
                // Calculate how many days to add to the current day (Sunday, Friday, Saturday go to next Monday)
                val addDays = when (fromDate.day) {
                    0 -> 1
                    5 -> 3
                    6 -> 2
                    else -> 1
                }
                SimpleDate(fromDate.year, fromDate.month, fromDate.day + addDays)
            }
            TaskConstants.REPEAT_SPAN.MONTH -> SimpleDate(fromDate.year, fromDate.month + 1, repeatItem.amount)
            // Else we are a specific month repeat so just add a year
            else -> SimpleDate(fromDate.year + 1, fromDate.month, fromDate.day)
        }
    }

    /**
     * Parse out the repeating text into a RepeatItem object
     */
    private fun parseRepeating(repeatText: String) : RepeatItem {
        console.log("repeatItemRegex: for repeatText", repeatText)
        val matches = TaskConstants.repeatItemRegex.find(repeatText)
        console.log("matches: ", matches)
        if (matches?.groupValues == null) {
            return RepeatItem(TaskConstants.REPEAT_SPAN.UNKNOWN, false, 0)
        }

        var index = 1
        val typeMatch = matches.groupValues[index++]
        val type = TaskConstants.REPEAT_SPAN.findForTag(typeMatch)
            ?: throw IllegalArgumentException("$typeMatch is not a valid repeat span")
        val fromComplete = matches.groupValues[index++] == "!"
        index++ // Skip the group for the optional repeat amount section (group 3)
        val amountValue = matches.groupValues[index]
        val amount = if (amountValue.isEmpty()) 0 else amountValue.toInt()

        return RepeatItem(type, fromComplete, amount)
    }
}
