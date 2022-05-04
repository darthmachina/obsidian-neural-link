@file:Suppress("RemoveRedundantQualifierName", "unused")

package neurallink.core.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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
        val expectedTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(
                DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)
                        to DataviewValue("monthly: 1")
            ).toDataviewMap(),
            dueOn = DueOn(LocalDate(2022, 1, 3))
        )

        val maybeLocalDate = getNextRepeatDate(expectedTask)
        val localDate = maybeLocalDate.shouldBeRight()
        localDate.year shouldBe 2022
        localDate.monthNumber shouldBe 2
        localDate.dayOfMonth shouldBe 3
    }

    // *** getNextRepeatTask() ***
    "getNextRepeatTask returns the right task" {
        val originalTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(
                DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)
                        to DataviewValue("monthly: 1")
            ).toDataviewMap(),
            dueOn = DueOn(LocalDate(2022, 1, 3))
        )

        val maybeRepeatTask = getNextRepeatingTask(originalTask)
        val task = maybeRepeatTask.shouldBeRight()
        task.completed.shouldBeFalse()
        task.completedOn.shouldBeNull()
        task.dueOn.shouldNotBeNull()
        task.dueOn!!.value.year shouldBe 2022
        task.dueOn!!.value.monthNumber shouldBe 2
        task.dueOn!!.value.dayOfMonth shouldBe 3
    }
})
