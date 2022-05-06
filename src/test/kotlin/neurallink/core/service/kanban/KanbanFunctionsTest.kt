package neurallink.core.service.kanban

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag
import neurallink.test.TestFactory

@Suppress("unused")
class KanbanFunctionsTest : StringSpec({
    // *** getStatusTagFromTask() ***
    "getStatusTagFromTask returns the correct tag" {
        val task = TestFactory.createTask().copy(
            tags = setOf(Tag("tag1"))
        )
        val statusTags = listOf(
            StatusTag(Tag("tag1"), "Tag 1"),
            StatusTag(Tag("tag2"), "Tag 2")
        )

        val maybeStatusTag = getStatusTagFromTask(task, statusTags)
        val actualStatusTag = maybeStatusTag.shouldBeRight()
        actualStatusTag.tag.value shouldBe "tag1"
    }

    "getStatusTagTask returns the correct warning if no tag exists" {
        val task = TestFactory.createTask()
        val statusTags = listOf(
            StatusTag(Tag("tag1"), "Tag 1"),
            StatusTag(Tag("tag2"), "Tag 2")
        )

        val maybeStatusTag = getStatusTagFromTask(task, statusTags)
        maybeStatusTag.shouldBeLeft()
    }

    "getStatusTagTask returns a tag if there is more than one status tag" {
        val task = TestFactory.createTask().copy(
            tags = setOf(Tag("tag1"), Tag("tag2"))
        )
        val statusTags = listOf(
            StatusTag(Tag("tag1"), "Tag 1"),
            StatusTag(Tag("tag2"), "Tag 2")
        )

        val maybeStatusTag = getStatusTagFromTask(task, statusTags)
        val actualStatusTag = maybeStatusTag.shouldBeRight()
        // It could be either tag, the behavior is not defined
        actualStatusTag.tag.value shouldBeIn setOf("tag1", "tag2")
    }

    // *** getAllStatusTagsOnTasks() ***
    "getAllStatusTagsOnTasks returns the correct set of tags" {
        val task1 = TestFactory.createTask().copy(
            tags = setOf(Tag("tag1"))
        )
        val task2 = TestFactory.createTask().copy(
            tags = setOf(Tag("tag2"))
        )
        val statusTags = listOf(
            StatusTag(Tag("tag1"), "Tag 1"),
            StatusTag(Tag("tag2"), "Tag 2")
        )

        val actualStatusTags = getAllStatusTagsOnTasks(listOf(task1, task2), statusTags)
        actualStatusTags.shouldContainAll(statusTags)
    }
})
