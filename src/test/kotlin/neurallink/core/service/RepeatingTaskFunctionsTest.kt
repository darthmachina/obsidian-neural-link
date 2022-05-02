@file:Suppress("RemoveRedundantQualifierName")

package neurallink.core.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import neurallink.core.model.*
import neurallink.test.TestFactory
import kotlin.math.exp

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
            dataviewFields = mapOf(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY) to DataviewValue("month: 1")).toDataviewMap()
        )

        val maybeMatches = findRepeatMatches(expectedTask)
        val matches = maybeMatches.shouldBeRight()
    }

    "findRepeatMatches returns an error if there is no repeat field" {
        val expectedTask = TestFactory.createTask()

        val maybeMatches = findRepeatMatches(expectedTask)
        maybeMatches.shouldBeLeft()
    }
})
