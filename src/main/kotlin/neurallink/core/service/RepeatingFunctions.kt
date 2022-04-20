package neurallink.core.service

import arrow.core.*
import kotlinx.datetime.*
import kotlinx.uuid.UUID
import model.RepeatItem
import neurallink.core.model.Task
import neurallink.core.model.TaskConstants

fun isTaskRepeating(task: Task) = task.dataviewFields.containsKey("repeat") && task.dueOn != null

fun repeatTask(task: Task) : Option<Task> {
    if (!isTaskRepeating(task)) {
        return none()
    }
    return Some(
        task.copy(
            id = UUID().toString(),
            dueOn = getNextRepeatDate(task),
            completed = false,
            completedOn = null
        )
    )
}

private fun getNextRepeatDate(task: Task) : LocalDate {
    val repeatItem = parseRepeating(task.dataviewFields["repeat"]!!)
    val fromDate = if (repeatItem.fromComplete) getCurrentDate() else task.dueOn!!

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

private fun getCurrentDate() : LocalDate {
    val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return LocalDate(currentTime.year, currentTime.month, currentTime.dayOfMonth)
}