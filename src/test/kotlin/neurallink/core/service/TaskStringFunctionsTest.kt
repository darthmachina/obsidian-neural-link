@file:Suppress("unused")

package neurallink.core.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import neurallink.core.model.*

class TaskStringFunctionsTest : StringSpec({
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
        val testTask = "- [ ] Task with completed @completed(2022-01-02T03:04:05)"

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
        val testTask = "- [ ] Task with completed @completed(2022-01-02T03:04)"

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
    "createTask should process a simple Task" {
        val testTask = "- [ ] Simple task"
        val testFilename = "testfile.md"
        val testLine = 2

        val actual = createTask(testFilename, testLine, testTask)

        // Check values that should be set
        actual.description shouldBe Description("Simple task")
        actual.file shouldBe TaskFile("testfile.md")
        actual.filePosition shouldBe FilePosition(2)
        actual.completed.shouldBeFalse()

        // Check values that should not be set
        actual.dueOn shouldBe null
        actual.completedOn shouldBe null
        actual.tags.shouldBeEmpty()
        actual.dataviewFields.shouldBeEmpty()
    }

    "createTask should process a Task with tags" {
        val testTask = "- [ ] Simple task with tag #tag1"
        val testFilename = "testfile.md"
        val testLine = 2

        val actual = createTask(testFilename, testLine, testTask)

        // Check values that should be set
        actual.description shouldBe Description("Simple task with tag")
        actual.file shouldBe TaskFile("testfile.md")
        actual.filePosition shouldBe FilePosition(2)
        actual.completed.shouldBeFalse()
        actual.tags.shouldHaveSize(1)
        actual.tags.shouldContain(Tag("tag1"))

        // Check values that should not be set
        actual.dueOn shouldBe null
        actual.completedOn shouldBe null
        actual.dataviewFields.shouldBeEmpty()
    }

    "createTask should process a Task with due date" {
        val testTask = "- [ ] Simple task with due date @due(2022-01-02)"
        val testFilename = "testfile.md"
        val testLine = 2

        val actual = createTask(testFilename, testLine, testTask)

        // Check values that should be set
        actual.description shouldBe Description("Simple task with due date")
        actual.file shouldBe TaskFile("testfile.md")
        actual.filePosition shouldBe FilePosition(2)
        actual.completed.shouldBeFalse()
        actual.dueOn shouldBe DueOn(LocalDate(year = 2022, monthNumber = 1, dayOfMonth = 2))

        // Check values that should not be set
        actual.completedOn shouldBe null
        actual.tags.shouldBeEmpty()
        actual.dataviewFields.shouldBeEmpty()
    }

    "createTask should process a Task with dataview fields" {
        val testTask = "- [ ] Simple task with dataview [dvfield:: dvvalue]"
        val testFilename = "testfile.md"
        val testLine = 2

        val actual = createTask(testFilename, testLine, testTask)

        // Check values that should be set
        actual.description shouldBe Description("Simple task with dataview")
        actual.file shouldBe TaskFile("testfile.md")
        actual.filePosition shouldBe FilePosition(2)
        actual.completed.shouldBeFalse()
        actual.dataviewFields.shouldHaveSize(1)
        actual.dataviewFields.shouldHaveKey(DataviewField("dvfield"))
        actual.dataviewFields[DataviewField("dvfield")] shouldBe DataviewValue("dvvalue")

        // Check values that should not be set
        actual.dueOn shouldBe null
        actual.completedOn shouldBe null
        actual.tags.shouldBeEmpty()
    }
})
