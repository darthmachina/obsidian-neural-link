package neurallink.core.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import arrow.core.rightIfNotNull
import kotlinx.datetime.*
import mu.KotlinLogging
import neurallink.core.model.*

private val logger = KotlinLogging.logger("RepeatingTaskFunctions")

/**
 * Detects whether the given task is a repeating task based on the `repeat` Dataview
 * field and an @due being set.
 */
fun isTaskRepeating(task: Task) : Boolean {
    logger.debug { "isTaskRepeating()" }
    return task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)) && task.dueOn != null
}

/**
 * Gets the repeated task to the given task. Will do the following:
 * 1. Remove the @completed tag that CardBoard adds when a task is completed
 * 2. Marks as incomplete
 * 3. Replaces the due date with the next date in the cycle
 */
fun getNextRepeatingTask(task: Task) : Either<NeuralLinkError,Task> {
    logger.debug { "getNextRepeatingTask()" }
    return getNextRepeatDate(task)
        .map { repeatDate ->
            task.copy(
                dueOn = DueOn(repeatDate),
                completed = false,
                completedOn = null
            )
        }
}

fun getNextDate(repeatItem: RepeatItem, fromDate: LocalDate) : LocalDate {
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

fun addTimeUntilDate(repeatItem: RepeatItem, fromDate: LocalDate, currentDate: LocalDate) : LocalDate{
    val nextDate = getNextDate(repeatItem, fromDate)
    return if (nextDate < currentDate) {
        addTimeUntilDate(repeatItem, nextDate, currentDate)
    } else {
        nextDate
    }
}

fun addTimeUntilFuture(repeatItem: RepeatItem, fromDate: LocalDate) : LocalDate {
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return addTimeUntilDate(repeatItem, fromDate, currentDate)
}

fun getNextRepeatDate(task: Task) : Either<NeuralLinkError,LocalDate> {
    return parseRepeating(task)
        .map { repeatItem ->
            Pair(repeatItem, getFromDate(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()), task.dueOn!!, repeatItem.fromComplete))
        }
        .map { repeatFromDatePair ->
            addTimeUntilFuture(repeatFromDatePair.first, repeatFromDatePair.second)
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
