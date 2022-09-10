package neurallink.core.service

import arrow.core.some
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import neurallink.core.model.DataviewField
import neurallink.core.model.DataviewPair
import neurallink.core.model.DataviewValue
import neurallink.core.model.DueOn
import neurallink.core.model.FilterOptions
import neurallink.core.model.Tag
import neurallink.core.model.TaskFile
import neurallink.core.store.DataviewFilterValue
import neurallink.core.store.FileFilterValue
import neurallink.core.store.TagFilterValue
import neurallink.test.TestFactory

@Suppress("unused")
class TaskFilterFunctionsTest : StringSpec({
    // pageInPathList()
    "pathInPathList finds exact match" {
        val pathToCheck = "test"
        val pathList = listOf("test")
        val actualCheck = pathInPathList(pathToCheck, pathList)
        actualCheck shouldBe true
    }

    "pathInPathList finds partial match" {
        val pathToCheck = "test/foo"
        val pathList = listOf("test")
        val actualCheck = pathInPathList(pathToCheck, pathList)
        actualCheck shouldBe true
    }

    "pathInPathList works with multiple paths in list" {
        val pathToCheck = "test"
        val pathList = listOf("test", "foo")
        val actualCheck = pathInPathList(pathToCheck, pathList)
        actualCheck shouldBe true
    }

    "pathInPathList returns false if no match" {
        val pathToCheck = "test"
        val pathList = listOf("foo")
        val actualCheck = pathInPathList(pathToCheck, pathList)
        actualCheck shouldBe false
    }

    "pathInPathList returns false if path contains but does not start with path in list" {
        val pathToCheck = "foo/test"
        val pathList = listOf("test")
        val actualCheck = pathInPathList(pathToCheck, pathList)
        actualCheck shouldBe false
    }

    // filterTasks()
    "filterTasks returns tasks from a single file" {
        val expectedTask = TestFactory.createTask(file = "testfile.md")
        val taskList = listOf(*TestFactory.createTasks(4).toTypedArray(), expectedTask)

        val actualTaskList = filterTasks(taskList, FilterOptions(page = FileFilterValue(TaskFile("testfile.md")).some()))
        actualTaskList shouldHaveSize 1
        actualTaskList[0].file shouldBe TaskFile("testfile.md")
    }

    "filterTasks returns tasks for a tag" {
        val expectedTask = TestFactory.createTask(tags = setOf(Tag("testtag")))
        val taskList = listOf(*TestFactory.createTasks(4).toTypedArray(), expectedTask)

        val actualTaskList = filterTasks(taskList, FilterOptions(tags = TagFilterValue(Tag("testtag")).some()))
        actualTaskList shouldHaveSize 1
        actualTaskList[0].tags shouldContain Tag("testtag")
    }

    "filterTasks returns tasks for a dataview value" {
        val expectedTask = TestFactory.createTask(
            description = "Expected",
            dataviewFields = mapOf(Pair(DataviewField("test"), DataviewValue(1)))
        )
        val taskList = listOf(*TestFactory.createTasks(4).toTypedArray(), expectedTask)

        val actualTaskList = filterTasks(taskList, FilterOptions(
            dataview = DataviewFilterValue(DataviewPair(Pair(DataviewField("test"), DataviewValue(1)))).some()))
        actualTaskList shouldHaveSize 1
        actualTaskList[0].description.value shouldBe "Expected"
    }

    "filterTasks returns tasks hiding future tasks" {
        val expectedTask = TestFactory.createTask(
            description = "Expected",
            dueOn = DueOn(LocalDate(2022, 1, 1))
        )
        val hiddenTask = TestFactory.createTask(
            description = "Hidden",
            dueOn = DueOn(LocalDate(2099, 1, 1))
        )
        val taskList = listOf(hiddenTask, expectedTask)

        val actualTaskList = filterTasks(taskList, FilterOptions(hideFuture = true))
        actualTaskList shouldHaveSize 1
        actualTaskList[0].description.value shouldBe "Expected"
    }

    "filterTasks returns tasks hiding future dates and from a single file" {
        val expectedTask = TestFactory.createTask(
            description = "Expected",
            file = "file1.md",
            dueOn = DueOn(LocalDate(2022, 1, 1))
        )
        val hiddenTask1 = TestFactory.createTask(
            description = "Hidden 1",
            file = "file1.md",
            dueOn = DueOn(LocalDate(2099, 1, 1))
        )
        val hiddenTask2 = TestFactory.createTask(
            description = "Hidden 2",
            file = "file2.md",
            dueOn = DueOn(LocalDate(2022, 1, 1))
        )
        val taskList = listOf(hiddenTask1, hiddenTask2, expectedTask)

        val actualTaskList = filterTasks(taskList, FilterOptions(
            hideFuture = true,
            page = FileFilterValue(TaskFile("file1.md")).some())
        )
        actualTaskList shouldHaveSize 1
        actualTaskList[0].description.value shouldBe "Expected"
    }
})
