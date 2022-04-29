package neurallink.core.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import neurallink.core.model.Description
import neurallink.test.TestListItemCache
import neurallink.test.TestLoc
import neurallink.test.TestPos

@Suppress("UNUSED_PARAMETER")
class VaultFunctionsTest : StringSpec ({
    // *** cacheItemParent() ***
    "cacheItemParent returns correct value" {
        val expectedParent = 5
        val listItemCache = TestListItemCache(
            expectedParent,
            TestPos(TestLoc.EMPTY, TestLoc.EMPTY)
        )

        val actualParent = cacheItemParent(listItemCache)
        actualParent shouldBe expectedParent
    }

    // *** cacheItemLine() ***
    "cacheItemLine returns start line" {
        val expectedLine = 8
        val listItemCache = TestListItemCache(
            -1,
            TestPos(TestLoc(expectedLine, 0, 0))
        )

        val actualLine = cacheItemLine(listItemCache)
        actualLine shouldBe expectedLine
    }

    // *** lineContents() ***
    "lineContents returns string version of the ListItemCache" {
        val expectedFileContents = listOf(
            "- [ ] First Line",
            "- [ ] Second Line",
            "- [ ] Third Line"
        )
        val expectedLineContents = "[ ] Second Line"
        val expectedLine = 1
        val listItemCache = TestListItemCache(
            -1,
            TestPos(TestLoc(expectedLine, 0, 0))
        )

        val actualLineContents = lineContents(expectedFileContents, listItemCache)
        actualLineContents shouldBe expectedLineContents
    }

    // *** noteFromLine() ***
    "noteFromLine should correctly process a string" {
        val fileContents = listOf(
            "- Don't want this note",
            "- Test note",
            "- [ ] Don't want this task"
        )
        val expectedLine = 1
        val expectedListItemCache = TestListItemCache(
            -1,
            TestPos(TestLoc(expectedLine, 0, 0))
        )

        val actualNote = noteFromLine(fileContents, expectedListItemCache)
        actualNote.note shouldBe "Test note"
        actualNote.subnotes.shouldBeEmpty()
    }

    // *** taskFromLine() ***
    "taskFromLine should correctly process a string" {
        val fileContents = listOf(
            "- Don't want this note",
            "- [ ] Test task",
            "- [ ] Don't want this task"
        )
        val expectedFilename = "test.md"
        val expectedLine = 1
        val expectedListItemCache = TestListItemCache(
            -1,
            TestPos(TestLoc(expectedLine, 0, 0))
        )

        val actualTask = taskFromLine(
            expectedFilename,
            fileContents,
            expectedListItemCache
        )
        actualTask.description shouldBe Description("Test task")
    }
})