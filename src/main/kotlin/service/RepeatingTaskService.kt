package service

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import model.RepeatItem
import neurallink.core.model.DataviewField
import neurallink.core.model.DueOn
import neurallink.core.model.Task
import neurallink.core.model.TaskConstants

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class RepeatingTaskService {
    /**
     * Detects whether the given task is a repeating task.
     */
    fun isTaskRepeating(task: Task) : Boolean {
        return task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)) && task.dueOn != null
    }

    /**
     * Gets the repeated task to the given task. Will do the following:
     * 1. Remove the @completed tag that CardBoard adds when a task is completed
     * 2. Marks as incomplete
     * 3. Replaces the due date with the next date in the cycle
     */
    fun getNextRepeatingTask(task: Task) : Task {
        console.log("getNextRepeatingTask()")
        val repeatTask = task.deepCopy()
        return repeatTask.copy(
            dueOn = DueOn(getNextRepeatDate(task)),
            completed = false,
            completedOn = null
        )
    }

    private fun getNextRepeatDate(task: Task) : LocalDate {
        // If there isn't a due date and repeating note then there is no next date
        if (!isTaskRepeating(task))
            throw IllegalArgumentException("Task requires a due date and repeating note to calculate next date\n\t$task")

        val repeatItem = parseRepeating(task.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)).asString())
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) // TODO Is TimeZone here going to affect anything?
        val fromDate = if (repeatItem.fromComplete) LocalDate(currentDate.year, currentDate.month, currentDate.dayOfMonth) else task.dueOn!!.value

        return when (repeatItem.span) {
            TaskConstants.REPEAT_SPAN.DAILY -> fromDate.plus(repeatItem.amount, DateTimeUnit.DAY)
            TaskConstants.REPEAT_SPAN.WEEKLY -> fromDate.plus(repeatItem.amount, DateTimeUnit.WEEK)
            TaskConstants.REPEAT_SPAN.MONTHLY -> fromDate.plus(repeatItem.amount, DateTimeUnit.MONTH)
            TaskConstants.REPEAT_SPAN.YEARLY -> fromDate.plus(repeatItem.amount, DateTimeUnit.YEAR)
            TaskConstants.REPEAT_SPAN.WEEKDAY -> {
                // Calculate how many days to add to the current day (Sunday, Friday, Saturday go to next Monday)
                val addDays = when (fromDate.dayOfMonth) {
                    0 -> 1
                    5 -> 3
                    6 -> 2
                    else -> 1
                }
                fromDate.plus(addDays, DateTimeUnit.DAY)
            }
            TaskConstants.REPEAT_SPAN.MONTH -> {
                fromDate.plus(1, DateTimeUnit.MONTH)
            }
            // Else we are a specific month repeat so just add a year
            else -> fromDate.plus(1, DateTimeUnit.YEAR)
        }
    }

    /**
     * Parse out the repeating text into a RepeatItem object
     */
    private fun parseRepeating(repeatText: String) : RepeatItem {
        console.log("repeatItemRegex: for repeatText", repeatText)
        val matches = TaskConstants.repeatItemRegex.find(repeatText)
//        console.log("matches: ", matches)
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
