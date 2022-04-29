package neurallink.core.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import neurallink.core.model.Description
import neurallink.test.TestFactory
import neurallink.test.TestListItemCache
import neurallink.test.TestLoc
import neurallink.test.TestPos
import kotlin.math.exp

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

    // *** buildNoteTree() ***
    "buildNoteTree creates a single Note" {
        val expectedNote = createNoteInProcess(2, 1)
        val expectedItemsInProcess = listOf(
            createNoteInProcess(0),
            createTaskInProcess(1),
            expectedNote,
            createNoteInProcess(3)
        )

        val actualNoteList = buildNoteTree(expectedItemsInProcess, 1)
        actualNoteList.shouldHaveSize(1)
        actualNoteList[0].note shouldBe expectedNote.note.note
        actualNoteList[0].subnotes.shouldBeEmpty()
    }

    "buildNoteTree creates a Note with a subnote" {
        val expectedNote = createNoteInProcess(2, 1)
        val expectedSubnote = createNoteInProcess(3, 2)
        val expectedItemsInProcess = listOf(
            createNoteInProcess(0),
            createTaskInProcess(1),
            expectedNote,
            expectedSubnote
        )

        val actualNoteList = buildNoteTree(expectedItemsInProcess, 1)
        actualNoteList.shouldHaveSize(1)
        actualNoteList[0].note shouldBe expectedNote.note.note
        actualNoteList[0].subnotes.shouldHaveSize(1)
        actualNoteList[0].subnotes[0].note shouldBe expectedSubnote.note.note
    }

    // *** buildTaskTree() ***
    "buildTaskTree creates a single Task" {
        val expectedTask = createTaskInProcess(2, 1)
        val expectedItemsInProcess = listOf(
            createNoteInProcess(0),
            createTaskInProcess(1),
            expectedTask,
            createNoteInProcess(3)
        )

        val actualTaskList = buildTTaskTree(expectedItemsInProcess, 1)
        actualTaskList.shouldHaveSize(1)
        actualTaskList[0].description shouldBe expectedTask.task.description
        actualTaskList[0].subtasks.shouldBeEmpty()
        actualTaskList[0].notes.shouldBeEmpty()
    }

    "buildTaskTree creates a Task with a subtask and a note" {
        val expectedTask = createTaskInProcess(2, 1)
        val expectedNote = createNoteInProcess(3, 2)
        val expectedSubtask = createTaskInProcess(4, 2)
        val expectedItemsInProcess = listOf(
            createNoteInProcess(0),
            createTaskInProcess(1),
            expectedTask,
            expectedNote,
            expectedSubtask,
            createTaskInProcess(5)
        )

        val actualTaskList = buildTTaskTree(expectedItemsInProcess, 1)
        actualTaskList.shouldHaveSize(1)
        actualTaskList[0].description shouldBe expectedTask.task.description
        actualTaskList[0].subtasks.shouldHaveSize(1)
        actualTaskList[0].subtasks[0].description shouldBe expectedSubtask.task.description
        actualTaskList[0].notes.shouldHaveSize(1)
        actualTaskList[0].notes[0].note shouldBe expectedNote.note.note
    }

    // *** buildRootTaskTree() ***
    "buildRootTaskTree builds a correct tree" {
        val expectedTaskOne = createTaskInProcess(1)
        val expectedTaskTwo = createTaskInProcess(2)
        val expectedSubtask = createTaskInProcess(3, 2)
        val expectedNote = createNoteInProcess(4, 2)
        val expectedTaskThree = createTaskInProcess(5)

        val expectedItemsInProcess = listOf(
            createNoteInProcess(0),
            expectedTaskOne,
            expectedTaskTwo,
            expectedSubtask,
            expectedNote,
            expectedTaskThree,
            createNoteInProcess(6)
        )

        val actualTaskList = buildRootTaskTree(expectedItemsInProcess)
        actualTaskList.shouldHaveSize(3)
        // Tasks should maintain file order, so check in order
        actualTaskList[0].description shouldBe expectedTaskOne.task.description
        actualTaskList[0].subtasks.shouldBeEmpty()
        actualTaskList[0].notes.shouldBeEmpty()
        actualTaskList[1].description shouldBe expectedTaskTwo.task.description
        actualTaskList[1].subtasks.shouldHaveSize(1)
        actualTaskList[1].subtasks[0].description shouldBe expectedSubtask.task.description
        actualTaskList[1].notes.shouldHaveSize(1)
        actualTaskList[1].notes[0].note shouldBe expectedNote.note.note
        actualTaskList[2].description shouldBe expectedTaskThree.task.description
        actualTaskList[2].subtasks.shouldBeEmpty()
        actualTaskList[2].notes.shouldBeEmpty()
    }
})

fun createTaskInProcess(position: Int = -1, parent: Int = -1) : TaskInProcess {
    val task = TestFactory.createTask(position)
    return TaskInProcess(
        task,
        task.filePosition.value,
        if (parent == -1) -task.filePosition.value else parent
    )
}

fun createNoteInProcess(position: Int = -1, parent: Int = -1) : NoteInProcess {
    val note = TestFactory.createNote(position)
    return NoteInProcess(
        note,
        note.filePosition.value,
        if (parent == -1) -note.filePosition.value else parent
    )
}
