@file:Suppress("RemoveRedundantQualifierName", "unused")

package neurallink.core.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.*
import neurallink.core.model.*
import neurallink.test.TestFactory

class RepeatingTaskFunctionsTest : StringSpec({
    // *** isTaskRepeating() ***
    "isTaskRepeating returns true if the field and due date exist" {
        val expectedTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY) to DataviewValue("test")).toDataviewMap(),
            dueOn = DueOn(LocalDate(2022, 1, 2))
        )

        isTaskRepeating(expectedTask).shouldBeTrue()
    }

    "isTaskRepeating returns false if neither the field nor due date exist" {
        val expectedTask = TestFactory.createTask()
        isTaskRepeating(expectedTask).shouldBeFalse()
    }

    "isTaskRepeating returns false if the field exists but not a due date" {
        val expectedTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY) to DataviewValue("test")).toDataviewMap(),
        )

        isTaskRepeating(expectedTask).shouldBeFalse()
    }

    "isTaskRepeating returns true if the field does not exist but a due date does" {
        val expectedTask = TestFactory.createTask().copy(
            dueOn = DueOn(LocalDate(2022, 1, 2))
        )

        isTaskRepeating(expectedTask).shouldBeFalse()
    }

    // *** getFromDate() ***
    "getFromDate gets current date if fromComplete is true" {
        val expectedDateTime = LocalDateTime(2023, 1, 2, 3, 4, 5)
        val expectedDueOn = DueOn(LocalDate(2022, 6, 7))

        val actualFromDate = getFromDate(expectedDateTime, expectedDueOn, true)
        actualFromDate.year shouldBe 2023
        actualFromDate.monthNumber shouldBe 1
        actualFromDate.dayOfMonth shouldBe 2
    }

    "getFromDate gets due date if fromComplete is false" {
        val expectedDateTime = LocalDateTime(2023, 1, 2, 3, 4, 5)
        val expectedDueOn = DueOn(LocalDate(2022, 6, 7))

        val actualFromDate = getFromDate(expectedDateTime, expectedDueOn, false)
        actualFromDate.year shouldBe 2022
        actualFromDate.monthNumber shouldBe 6
        actualFromDate.dayOfMonth shouldBe 7
    }

    // *** findRepeatMatches() ***
    "findRepeatMatches gets the results if the field exists" {
        val expectedTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(
                DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)
                        to DataviewValue("month: 1")
            ).toDataviewMap()
        )

        val maybeMatches = findRepeatMatches(expectedTask)
        val matches = maybeMatches.shouldBeRight()
        matches.groupValues[1] shouldBe "month"
        matches.groupValues[2] shouldBe ""
        matches.groupValues[4] shouldBe "1"
    }

    "findRepeatMatches returns an error if there is no repeat field" {
        val expectedTask = TestFactory.createTask()

        val maybeMatches = findRepeatMatches(expectedTask)
        maybeMatches.shouldBeLeft()
    }

    // ** parseRepeating() ***
    "parseRepeating parses a month: 1 value correctly" {
        val expectedTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(
                DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)
                        to DataviewValue("monthly: 1")
            ).toDataviewMap()
        )

        val maybeRepeatItem = parseRepeating(expectedTask)
        val repeatItem = maybeRepeatItem.shouldBeRight()
        repeatItem.span shouldBe TaskConstants.REPEAT_SPAN.MONTHLY
        repeatItem.amount shouldBe 1
        repeatItem.fromComplete.shouldBeFalse()
    }

    "parseRepeating should return an Error if the value is invalid" {
        val expectedTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(
                DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)
                        to DataviewValue("invalid: 1")
            ).toDataviewMap()
        )

        val maybeRepeatItem = parseRepeating(expectedTask)
        val error = maybeRepeatItem.shouldBeLeft()
        (error is RepeatTaskParseError).shouldBeTrue()
    }

    // TODO Create more tests around repeat values

    // *** getNextRepeatDate() ***
    "getNextRepeatDate returns the right date" {
        val dueDate = Clock.System.now().toLocalDateTime(
            TimeZone.currentSystemDefault()
        ).date.minus(1, DateTimeUnit.DAY)
        val expectedYear = dueDate.year
        val expectedMonth = dueDate.monthNumber + 1
        val expectedDay = dueDate.dayOfMonth
        val expectedTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(
                DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)
                        to DataviewValue("monthly: 1")
            ).toDataviewMap(),
            dueOn = DueOn(dueDate)
        )

        val maybeLocalDate = getNextRepeatDate(expectedTask)
        val localDate = maybeLocalDate.shouldBeRight()
        withClue("Year is wrong") { localDate.year shouldBe expectedYear }
        withClue("Month is wrong") { localDate.monthNumber shouldBe expectedMonth }
        withClue("Day is wrong") { localDate.dayOfMonth shouldBe expectedDay }
    }

    "getNextRepeatDate returns a date in the future" {
        val currentDate = Clock.System.now().toLocalDateTime(
            TimeZone.currentSystemDefault()
        ).date
        val expectedYear = currentDate.year
        val expectedMonth = currentDate.monthNumber
        val expectedDay = currentDate.dayOfMonth + 1
        val expectedTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(
                DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)
                        to DataviewValue("monthly: 1")
            ).toDataviewMap(),
            dueOn = DueOn(LocalDate(
                currentDate.year,
                currentDate.monthNumber - 2,
                currentDate.dayOfMonth + 1))
        )

        val maybeLocalDate = getNextRepeatDate(expectedTask)
        val localDate = maybeLocalDate.shouldBeRight()
        withClue("Year is wrong") { localDate.year shouldBe expectedYear }
        withClue("Month is wrong") { localDate.monthNumber shouldBe expectedMonth }
        withClue("Day is wrong") { localDate.dayOfMonth shouldBe expectedDay }
    }

    // *** uncompleteSubtasks() ***
    "uncompleteSubtasks unchecks all subtasks on task" {
        val subtasks = TestFactory.createTasks(3)
            .map { task ->
                task.copy(completed = true)
            }
        val originalTask = TestFactory.createTask()
            .copy(
                subtasks = subtasks
            )

        val actualSubtasks = uncompleteSubtasks(originalTask)
        actualSubtasks.forEach { subtask ->
            subtask.completed shouldBe false
        }
    }

    // *** getNextRepeatTask() ***
    "getNextRepeatTask returns the right task" {
        val dueDate = Clock.System.now().toLocalDateTime(
            TimeZone.currentSystemDefault()
        ).date.minus(1, DateTimeUnit.DAY)
        val expectedYear = dueDate.year
        val expectedMonth = dueDate.monthNumber + 1
        val expectedDay = dueDate.dayOfMonth
        val originalTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(
                DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)
                        to DataviewValue("monthly: 1")
            ).toDataviewMap(),
            dueOn = DueOn(dueDate)
        )

        val maybeRepeatTask = getNextRepeatingTask(originalTask)
        val task = maybeRepeatTask.shouldBeRight()
        task.completed.shouldBeFalse()
        task.completedOn.shouldBeNull()
        task.dueOn.shouldNotBeNull()
        withClue("Task year is wrong") { task.dueOn!!.value.year shouldBe expectedYear }
        withClue("Task month is wrong") { task.dueOn!!.value.monthNumber shouldBe expectedMonth }
        withClue("Task day is wrong") { task.dueOn!!.value.dayOfMonth shouldBe expectedDay }
    }

    "getNextRepeatTask returns task with subtasks not completed" {
        val dueDate = Clock.System.now().toLocalDateTime(
            TimeZone.currentSystemDefault()
        ).date.minus(1, DateTimeUnit.DAY)
        val subtasks = TestFactory.createTasks(3)
            .map { task ->
                task.copy(completed = true)
            }
        val originalTask = TestFactory.createTask()
            .copy(
                dataviewFields = mapOf(
                    DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)
                            to DataviewValue("monthly: 1")
                ).toDataviewMap(),
                dueOn = DueOn(dueDate),
                subtasks = subtasks
            )
        val maybeRepeatTask = getNextRepeatingTask(originalTask)
        val task = maybeRepeatTask.shouldBeRight()
        task.subtasks.forEach { subtask ->
            subtask.completed shouldBe false
        }
    }
})
