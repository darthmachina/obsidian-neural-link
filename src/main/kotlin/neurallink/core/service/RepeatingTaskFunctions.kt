package neurallink.core.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import arrow.core.rightIfNotNull
import kotlinx.datetime.*
import neurallink.core.model.*

/**
 * Detects whether the given task is a repeating task based on the `repeat` Dataview
 * field and an @due being set.
 */
fun isTaskRepeating(task: Task) : Boolean {
    console.log("isTaskRepeating()")
    return task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)) && task.dueOn != null
}

/**
 * Gets the repeated task to the given task. Will do the following:
 * 1. Remove the @completed tag that CardBoard adds when a task is completed
 * 2. Marks as incomplete
 * 3. Replaces the due date with the next date in the cycle
 */
fun getNextRepeatingTask(task: Task) : Either<NeuralLinkError,Task> {
    console.log("getNextRepeatingTask()")
    return getNextRepeatDate(task)
        .map { repeatDate ->
            task.copy(
                dueOn = DueOn(repeatDate),
                completed = false,
                completedOn = null
            )
        }
}

fun getNextRepeatDate(task: Task) : Either<NeuralLinkError,LocalDate> {
    return parseRepeating(task)
        .map { repeatItem ->
            Pair(repeatItem, getFromDate(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()), task.dueOn!!, repeatItem.fromComplete))
        }
        .map { repeatFromDatePair ->
            when (repeatFromDatePair.first.span) {
                TaskConstants.REPEAT_SPAN.DAILY -> repeatFromDatePair.second.plus(repeatFromDatePair.first.amount, DateTimeUnit.DAY)
                TaskConstants.REPEAT_SPAN.WEEKLY -> repeatFromDatePair.second.plus(repeatFromDatePair.first.amount, DateTimeUnit.WEEK)
                TaskConstants.REPEAT_SPAN.MONTHLY -> repeatFromDatePair.second.plus(repeatFromDatePair.first.amount, DateTimeUnit.MONTH)
                TaskConstants.REPEAT_SPAN.YEARLY -> repeatFromDatePair.second.plus(repeatFromDatePair.first.amount, DateTimeUnit.YEAR)
                TaskConstants.REPEAT_SPAN.WEEKDAY -> {
                    // Calculate how many days to add to the current day (Sunday, Friday, Saturday go to next Monday)
                    val addDays = when (repeatFromDatePair.second.dayOfMonth) {
                        0 -> 1
                        5 -> 3
                        6 -> 2
                        else -> 1
                    }
                    repeatFromDatePair.second.plus(addDays, DateTimeUnit.DAY)
                }
                TaskConstants.REPEAT_SPAN.MONTH -> {
                    repeatFromDatePair.second.plus(1, DateTimeUnit.MONTH)
                }
                // Else we are a specific month repeat so just add a year
                else -> repeatFromDatePair.second.plus(1, DateTimeUnit.YEAR)
            }
        }
}

fun getFromDate(currentDate: LocalDateTime, dueOn: DueOn, fromComplete: Boolean) : LocalDate {
    return if (fromComplete) LocalDate(currentDate.year, currentDate.month, currentDate.dayOfMonth) else dueOn.value
}

fun parseRepeating(task: Task) : Either<NeuralLinkError,RepeatItem> {
    return findRepeatMatches(task)
        .flatMap {
            it.groupValues.rightIfNotNull {
                RepeatTaskParseError("groupValues is null for repeat property")
            }
        }
        .flatMap {
            Either.catch {
                RepeatItem(
                    TaskConstants.REPEAT_SPAN.findForTag(it[1]),
                    it[2] == "!",
                    it[4].toInt()
                )
            }.mapLeft {
                RepeatTaskParseError("Error creating RepeatItem", it)
            }
        }
}

fun findRepeatMatches(task: Task) : Either<NeuralLinkError,MatchResult> {
    return task.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY))
        .flatMap {
            TaskConstants.repeatItemRegex.find(it.asString()).right()
        }
        .flatMap {
            it.rightIfNotNull {
                RepeatTaskParseError("No matches found for repeat property")
            }
        }
}
