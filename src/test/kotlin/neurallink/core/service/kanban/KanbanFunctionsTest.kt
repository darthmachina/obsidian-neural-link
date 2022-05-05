package neurallink.core.service.kanban

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.StringSpec
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag
import neurallink.test.TestFactory

@Suppress("unused")
class KanbanFunctionsTest : StringSpec({
    // *** getStatusTagFromTask() ***
    "getStatusTagFromTask returns the correct tag" {

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

    }

    // *** getAllStatusTagsOnTasks() ***
    "getAllStatusTagsOnTasks returns the correct set of tags" {

    }
})
