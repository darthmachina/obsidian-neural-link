package neurallink.core.service.kanban

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.uuid.UUID
import neurallink.core.model.*
import neurallink.test.TestFactory

class TaskOrderFunctonsTest : StringSpec({
    // *** findMaxPositionInStatusTasks() ***
    "findMaxPositionInStatusTasks finds the correct position" {
        val task1 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(1.0)).toDataviewMap()
        )
        val task2 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(2.0)).toDataviewMap()
        )

        val maxPosition = findMaxPositionInStatusTasks(listOf(task1, task2))
        maxPosition shouldBe 2.0
    }

    "findMaxPositionInStatusTasks works with an empty task list" {
        val maxPosition = findMaxPositionInStatusTasks(emptyList())
        maxPosition shouldBe 1.0
    }

    // *** findPositionBeforeTask() ***
    "findPositionBeforeTask finds the correct position between tasks" {
        val task1 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(1.0)).toDataviewMap()
        )
        val task2 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(2.0)).toDataviewMap()
        )

        val maybePosition = findPositionBeforeTask(listOf(task1, task2), task2.id)
        val actualPosition = maybePosition.shouldBeRight()
        actualPosition shouldBe 1.5
    }

    "findPositionBeforeTask finds the correct position at beginning" {
        val task1 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(1.0)).toDataviewMap()
        )
        val task2 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(2.0)).toDataviewMap()
        )

        val maybePosition = findPositionBeforeTask(listOf(task1, task2), task1.id)
        val actualPosition = maybePosition.shouldBeRight()
        actualPosition shouldBe 0.5
    }

    "findPositionBeforeTask returns error if beforeTaskId does not exist" {
        val task1 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(1.0)).toDataviewMap()
        )
        val task2 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(2.0)).toDataviewMap()
        )

        val maybePosition = findPositionBeforeTask(listOf(task1, task2), TaskId(UUID()))
        maybePosition.shouldBeLeft()
    }

    // *** findPosition() ***
    "findPosition finds the correct position" {
        val task1 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(1.0)).toDataviewMap(),
            tags = setOf(Tag("tag1"))
        )
        val task2 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(2.0)).toDataviewMap(),
            tags = setOf(Tag("tag1"))
        )
        val ignoredTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(4.0)).toDataviewMap(),
            tags = setOf(Tag("tag2"))
        )
        val statusTag = StatusTag(Tag("tag1"), "Tag 1")

        val maybePosition = findPosition(listOf(task1, task2, ignoredTask), statusTag, task2.id)
        val actualPosition = maybePosition.shouldBeRight()
        actualPosition shouldBe 1.5
    }

    // TODO Add more findPosition tests

    // *** findEndPosition() ***
    "findEndPosition finds the correct position" {
        val task1 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(1.0)).toDataviewMap(),
            tags = setOf(Tag("tag1"))
        )
        val task2 = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(2.0)).toDataviewMap(),
            tags = setOf(Tag("tag1"))
        )
        val ignoredTask = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(4.0)).toDataviewMap(),
            tags = setOf(Tag("tag2"))
        )
        val statusTag = StatusTag(Tag("tag1"), "Tag 1")

        val endPosition = findEndPosition(listOf(task1, task2, ignoredTask), statusTag)
        endPosition shouldBe 3.0
    }

    // *** addOrderToListItemsIfNeeded() ***
    "addOrderToListItemsIfNeeded adds position to correct tasks" {
        val taskWithOrder = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(1.0)).toDataviewMap()
        )
        val taskWithoutOrder = TestFactory.createTask()

        val actualTaskList = addOrderToListItemsIfNeeded(listOf(taskWithOrder, taskWithoutOrder))

        actualTaskList shouldHaveSize 2
        val updatedTask = actualTaskList.find { it.id == taskWithoutOrder.id }
        updatedTask shouldNotBe null
        updatedTask?.dataviewFields?.shouldHaveSize(1)
        updatedTask
            ?.dataviewFields
            ?.get(DataviewField(TaskConstants.TASK_ORDER_PROPERTY))
            ?.shouldBe(DataviewValue(2.0))
    }

    // *** updateTaskOrder() ***
    "updateTaskOrder takes task with order and returns updated order" {
        val taskWithOrder = TestFactory.createTask().copy(
            dataviewFields = mapOf(TestFactory.createDataviewPosition(1.0)).toDataviewMap()
        )

        val actualTask = updateTaskOrder(taskWithOrder, 2.0)
        actualTask.dataviewFields[DataviewField(TaskConstants.TASK_ORDER_PROPERTY)]
            ?.asDouble() shouldBe 2.0
    }

    "updateTaskOrder takes task without order and returns task with order" {
        val taskWithoutOrder = TestFactory.createTask()

        val actualTask = updateTaskOrder(taskWithoutOrder, 2.0)
        actualTask.dataviewFields[DataviewField(TaskConstants.TASK_ORDER_PROPERTY)]
            ?.asDouble() shouldBe 2.0
    }
})
