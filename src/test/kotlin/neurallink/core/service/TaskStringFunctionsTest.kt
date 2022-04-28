@file:Suppress("unused")

package neurallink.core.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Month

class TaskStringFunctionsTest : StringSpec({
    "tests should run" {
        1 + 2 shouldBe 3
    }

    // *** getDueDate() ***
    "getDueDate should parse correctly" {
        val testTask = "- [ ] Testing due dates @due(2022-01-02)"

        val maybeLocalDate = getDueDate(testTask)
        val localDate = maybeLocalDate.shouldBeRight()
        localDate shouldNotBe null
        localDate.year shouldBe 2022
        localDate.month shouldBe Month.JANUARY
        localDate.dayOfMonth shouldBe 2
    }

    "getDueDate should return an Either.Left if there is no due date" {
        val testTask = "- [ ] There is no due date"

        val maybeLocalDate = getDueDate(testTask)
        maybeLocalDate.shouldBeLeft()
    }

    // *** getCompletedDate() ***
    "getCompletedDate should parse correctly" {
        val testTask = "- [ ] Task with completed @completed(2022-01-02T03:04:05"

        val maybeLocalDateTime = getCompletedDate(testTask)
        val localDateTime = maybeLocalDateTime.shouldBeRight()
        localDateTime shouldNotBe null
        localDateTime.year shouldBe 2022
        localDateTime.month shouldBe Month.JANUARY
        localDateTime.dayOfMonth shouldBe 2
        localDateTime.hour shouldBe 3
        localDateTime.minute shouldBe 4
        localDateTime.second shouldBe 5
    }

    "getCompletedDate should parse correctly without seconds" {
        val testTask = "- [ ] Task with completed @completed(2022-01-02T03:04"

        val maybeLocalDateTime = getCompletedDate(testTask)

        val localDateTime = maybeLocalDateTime.shouldBeRight()
        localDateTime shouldNotBe null
        localDateTime.year shouldBe 2022
        localDateTime.month shouldBe Month.JANUARY
        localDateTime.dayOfMonth shouldBe 2
        localDateTime.hour shouldBe 3
        localDateTime.minute shouldBe 4
        localDateTime.second shouldBe 0
    }

    "getCompletedDate should return an Either.Left if there is no due date" {
        val testTask = "- [ ] There is no completed date"

        val maybeLocalDate = getCompletedDate(testTask)
        maybeLocalDate.shouldBeLeft()
    }

    // *** createTask() ***
})
