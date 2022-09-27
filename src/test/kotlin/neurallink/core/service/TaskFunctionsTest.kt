package neurallink.core.service

import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.uuid.UUID
import neurallink.core.model.DataviewField
import neurallink.core.model.DataviewValue
import neurallink.core.model.Description
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag
import neurallink.core.model.TaskFile
import neurallink.core.model.TaskId
import neurallink.test.TestFactory

@Suppress("unused")
class TaskFunctionsTest : StringSpec({
    "changedTasks only returns updated tasks" {
        val vaultTasks = TestFactory.createTasks(5)
            .map { task ->
                task.copy(file = TaskFile("testfile.md"))
            }
        val fileTasks = vaultTasks.map {
            it.deepCopy()
        }.toMutableList()
        fileTasks[0] = fileTasks[0].copy(
            description = Description("Changed"),
            id = TaskId(UUID())
        )

        val actualTasks = changedTasks("testfile.md", fileTasks, vaultTasks)
        val modifiedTasks = actualTasks.shouldBeSome()
        modifiedTasks.modified shouldHaveSize 1
        modifiedTasks.modified[0].description.value shouldBe "Changed"
        modifiedTasks.removed shouldBe false
    }

    "changedTasks works with tasks from multiple files" {
        val file1Tasks = TestFactory.createTasks(3)
            .map { task ->
                task.copy(file = TaskFile("testfile1.md"))
            }
        val file2Tasks = TestFactory.createTasks(3)
            .map { task ->
                task.copy(file = TaskFile("testfile2.md"))
            }
        val vaultTasks = file1Tasks + file2Tasks
        val fileTasks = vaultTasks
            .filter { it.file.value == "testfile1.md" }
            .map {
                it.deepCopy()
            }.toMutableList()
        fileTasks[0] = fileTasks[0].copy(
            description = Description("Changed"),
            id = TaskId(UUID())
        )

        val actualTasks = changedTasks("testfile1.md", fileTasks, vaultTasks)
        val modifiedTasks = actualTasks.shouldBeSome()
        modifiedTasks.modified shouldHaveSize 1
        modifiedTasks.modified[0].description.value shouldBe "Changed"
        modifiedTasks.removed shouldBe false
    }

    "changedTasks works with a fully populated Task" {
        val task1 = TestFactory.createFullTask(1).copy(file = TaskFile("testfile1.md"))
        val task2 = TestFactory.createFullTask(4).copy(file = TaskFile("testfile1.md"))
        val changedTask = task1.deepCopy().copy(
            description = Description("Changed"),
            id = TaskId(UUID())
        )

        val vaultTasks = listOf(task1, task2)
        val fileTasks = listOf(task1, changedTask)

        val actualTasks = changedTasks("testfile1.md", fileTasks, vaultTasks)
        val modifiedTasks = actualTasks.shouldBeSome()
        modifiedTasks.modified shouldHaveSize 1
        modifiedTasks.modified[0].description.value shouldBe "Changed"
        modifiedTasks.removed shouldBe false
    }

    "taskContainsAnyStatusTag returns true if tag is on task" {
        val task = TestFactory.createTask(tags = setOf(Tag("status")))
        val result = taskContainsAnyStatusTag(task, listOf(StatusTag(Tag("status"), "Status")))
        result shouldBe true
    }

    "taskContainsAnyStatusTag returns false if tag is not on task" {
        val task = TestFactory.createTask()
        val result = taskContainsAnyStatusTag(task, listOf(StatusTag(Tag("status"), "Status")))
        result shouldBe false
    }

    "taskContainsDataviewField returns true if field is on task" {
        val fieldMap = mapOf(Pair(DataviewField("pos"), DataviewValue(1.0)))
        val task = TestFactory.createTask(dataviewFields = fieldMap)
        val result = taskContainsDataviewField(task, DataviewField("pos"))
        result shouldBe true
    }

    "taskContainsDataviewField returns false if field is not on task" {
        val task = TestFactory.createTask()
        val result = taskContainsDataviewField(task, DataviewField("pos"))
        result shouldBe false
    }
})
