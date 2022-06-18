package neurallink.core.service

import arrow.core.Either
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import neurallink.core.model.*

private val dueDateRegex = Regex("""@${TaskConstants.DUE_ON_PROPERTY}${TaskConstants.TASK_PAPER_DATE_FORMAT}""")
private val completedDateRegex = Regex("""@${TaskConstants.COMPLETED_ON_PROPERTY}${TaskConstants.TASK_PAPER_DATE_FORMAT}""")
private val allTagsRegex = Regex(TaskConstants.ALL_TAGS_REGEX)
private val dataviewRegex = Regex(TaskConstants.DATAVIEW_REGEX)
private val whitespaceRegex = Regex("""\s+""")
private val taskTagRegex = Regex("""- \[[Xx ]\] """)
private val completedRegex = Regex(TaskConstants.COMPLETED_REGEX)

fun createTask(
    file: String,
    line: Int,
    text: String
) : Task {
    return Task(
        TaskFile(file),
        FilePosition(line),
        Description(text
            .replace(dueDateRegex, "")
            .replace(completedDateRegex, "")
            .replace(allTagsRegex, "")
            .replace(dataviewRegex, "")
            .trim()
            .replace("""\s+""".toRegex(), " ")
            .replace("""- \[[Xx ]\] """.toRegex(), "")),
        getDueDate(text).fold(
            ifLeft = { null },
            ifRight = { DueOn(it) }
        ),
        getCompletedDate(text).fold(
            ifLeft = { null },
            ifRight = { CompletedOn(it) }
        ),
        allTagsRegex.findAll(text).map { Tag(it.groupValues[1]) }.toSet(),
        dataviewRegex.findAll(text).associate {
            val doubleValue = it.groupValues[2].toDoubleOrNull()
            if (doubleValue != null) {
                DataviewField(it.groupValues[1]) to DataviewValue(doubleValue)
            } else {
                // Just use the value
                DataviewField(it.groupValues[1]) to DataviewValue(it.groupValues[2])
            }
        }.toDataviewMap(),
        completedRegex.containsMatchIn(text)
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
fun getDueDate(task: String) : Either<TaskReadingWarning, LocalDate> {
    val dateMatch = dueDateRegex.find(task)
    return if (dateMatch == null) {
        Either.Left(TaskReadingWarning("Due Date not found on task"))
    } else {
        val dateSplit = dateMatch.groupValues[1].split('-')
        Either.Right(LocalDate(
            dateSplit[0].toInt(),
            dateSplit[1].toInt(),
            dateSplit[2].toInt()
        ))
    }
}

/**
 * Gets the completed date from the task string
 *
 * So, given `Task @completed(2021-01-01)`, this will return a Date object set to`2021-01-01`
 * TODO Support Dataview style inline field for Completed Date
 *
 * @param task The task String
 * @return A DateTime object representing the completed date or null if no completed date is present
 */
fun getCompletedDate(task: String) : Either<TaskReadingWarning, LocalDateTime> {
    val dateMatch = completedDateRegex.find(task)
    return if (dateMatch == null) {
        Either.Left(TaskReadingWarning("Completed Date not found on task"))
    } else {
        val dateAndTime = dateMatch.groupValues[1].split("T")
        val dateSplit = dateAndTime[0].split('-')
        val timeSplit = dateAndTime[1].split(':')
        Either.Right(LocalDateTime(
            dateSplit[0].toInt(), // Year
            dateSplit[1].toInt(), // Month
            dateSplit[2].toInt(), // Day
            timeSplit[0].toInt(), // Hour
            timeSplit[1].toInt(), // Minute
            if (timeSplit.size == 3) timeSplit[2].toInt() else 0 // Second
        ))
    }
}