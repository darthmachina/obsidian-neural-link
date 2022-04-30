@file:Suppress("RemoveRedundantQualifierName", "UNUSED_PARAMETER")

package neurallink.core.service

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import neurallink.core.model.Description
import neurallink.test.TestFactory
import neurallink.test.TestListItemCache
import neurallink.test.TestLoc
import neurallink.test.TestPos

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

    // *** indentedCount() ***
    "indentedCount returns 0 if there are no subtasks or notes" {
        val expectedTask = TestFactory.createTask()

        val actualIndentedCount = indentedCount(expectedTask)
        actualIndentedCount shouldBe 0
    }

    "indentedCount returns 1 if there is a subtask" {
        val expectedTask = TestFactory.createTask().copy(
            subtasks = listOf(TestFactory.createTask())
        )


        val actualIndentedCount = indentedCount(expectedTask)
        actualIndentedCount shouldBe 1
    }

    "indentedCont returns 2 if there is a subtask and a note" {
        val expectedTask = TestFactory.createTask().copy(
            subtasks = listOf(TestFactory.createTask()),
            notes = listOf(TestFactory.createNote())
        )

        val actualIndentedCount = indentedCount(expectedTask)
        actualIndentedCount shouldBe 2
    }

    "indentedCount returns 4 if there is subtask with a subtask and a note with a note" {
        val expectedTask = TestFactory.createTask().copy(
            subtasks = listOf(
                TestFactory.createTask().copy(
                    subtasks = listOf(TestFactory.createTask())
                )
            ),
            notes = listOf(
                TestFactory.createNote().copy(
                    subnotes = listOf(TestFactory.createNote())
                )
            )
        )

        val actualIndentedCount = indentedCount(expectedTask)
        actualIndentedCount shouldBe 4
    }

    // *** indentedNoteCount() ***
    "indentedNoteCount returns 0 if there are no subnotes" {
        val expectedNote = TestFactory.createNote()

        val actualIndentedCount = indentedNoteCount(expectedNote)
        actualIndentedCount shouldBe 0
    }

    "indentedNoteCount returns 1 if there is a subnote" {
        val expectedNote = TestFactory.createNote().copy(
            subnotes = listOf(TestFactory.createNote())
        )

        val actualIndentedCount = indentedNoteCount(expectedNote)
        actualIndentedCount shouldBe 1
    }

    // *** expandRemovalLines() ***
    "expandRemovalLines calculates the correct value" {
        // Example Task
        // ------------
        // 5: - [ ] Test task
        // 6:   - Indented 1
        // 7:   - Indented 2
        // 8:   - Indented 3
        // 9:   - Indented 4
        val lineNumber = 5
        val indentedCount = 4
        val expectedLinesToRemove = listOf(6, 7, 8, 9)

        val actualLinesToRemove = expandRemovalLines(lineNumber, indentedCount)
        actualLinesToRemove shouldBe expectedLinesToRemove
    }

    // *** joinFileContentsWithTasks() ***
    "joinFileContentsWithTasks collates the data correctly" {
        // Example File
        // ------------
        // 0: - Note 1
        // 1: - [ ] Task 1
        // 2:   - [ ] Subtask 1
        // 3:   - Subnote 1
        // 4: - [ ] Task 2
        // We will be replacing Task 1 on line 1 and both indented items, though
        //  the Task details will be randomized by the TestFactory
        val existingContents = listOf(
            "- Note 1",
            "- [ ] Task 1",
            "  - [ ] Subtask 1",
            "  - Subnote 1",
            "- [ ] Task 2"
        )
        val expectedTask = TestFactory.createTask(1).copy(
            subtasks = listOf(TestFactory.createTask(2)),
            notes = listOf(TestFactory.createNote(3))
        )

        val actualJoinedMap = joinFileContentsWithTasks(existingContents, listOf(expectedTask))
        actualJoinedMap.shouldHaveSize(5)
        actualJoinedMap[0]!!.first shouldBe "- Note 1"
        actualJoinedMap[0]!!.second shouldBe null
        actualJoinedMap[0]!!.third shouldBe null
        actualJoinedMap[1]!!.first shouldBe "- [ ] Task 1"
        actualJoinedMap[1]!!.second shouldBe expectedTask
        actualJoinedMap[1]!!.third shouldBe listOf(2, 3)
        actualJoinedMap[2]!!.first shouldBe "  - [ ] Subtask 1"
        actualJoinedMap[2]!!.second shouldBe null
        actualJoinedMap[2]!!.third shouldBe null
        actualJoinedMap[3]!!.first shouldBe "  - Subnote 1"
        actualJoinedMap[3]!!.second shouldBe null
        actualJoinedMap[3]!!.third shouldBe null
        actualJoinedMap[4]!!.first shouldBe "- [ ] Task 2"
        actualJoinedMap[4]!!.second shouldBe null
        actualJoinedMap[4]!!.third shouldBe null
    }

    "markRemoveLines marks the correct lines in the data" {
        // Example File
        // ------------
        // 0: - Note 1
        // 1: - [ ] Task 1
        // 2:   - [ ] Subtask 1
        // 3:   - Subnote 1
        // 4: - [ ] Task 2
        // We will be replacing Task 1 on line 1 and both indented items, though
        //  the Task details will be randomized by the TestFactory
        val inputMap = mapOf(
            0 to Triple("- Note 1", null, false),
            1 to Triple("- [ ] Task 1", listOf(1,2,3), true),
            2 to Triple("  - [ ] Subtask 1", null, false),
            3 to Triple("  - Subnote 1", null, false),
            4 to Triple("- [ ] Task 2", null, false),
        )

        val maybeLines = inputMap.markRemoveLines()
        val lines = maybeLines
            .shouldBeRight()
        lines.shouldHaveSize(5)
        lines[0].first shouldBe "- Note 1"
        lines[0].second.shouldBeFalse()
        lines[1].first shouldBe "- [ ] Task 1"
        lines[1].second.shouldBeTrue()
        lines[2].first shouldBe "  - [ ] Subtask 1"
        lines[2].second.shouldBeTrue()
        lines[3].first shouldBe "  - Subnote 1"
        lines[3].second.shouldBeTrue()
        lines[4].first shouldBe "- [ ] Task 2"
        lines[4].second.shouldBeFalse()
    }

    "createFileContents should create the correct markdown string" {
        // Example File
        // ------------
        // 0: - Note 1
        // 1: - [ ] Task 1
        // 2:   - [ ] Subtask 1
        // 3:   - Subnote 1
        // 4: - [ ] Task 2
        // We will be replacing Task 1 on line 1 and both indented items, though
        //  the Task details will be randomized by the TestFactory
        val existingContents =
            "- Note 1\n- [ ] Task 1\n  - [ ] Subtask 1\n  - Subnote 1\n- [ ] Task 2"
        val expectedSubtask = TestFactory.createTask(2)
        val expectedNote = TestFactory.createNote(3)
        val expectedTask = TestFactory.createTask(1).copy(
            subtasks = listOf(expectedSubtask),
            notes = listOf(expectedNote)
        )
        val expectedString =
            "- Note 1\n- [ ] ${expectedTask.description.value} \n\t- [ ] ${expectedSubtask.description.value} \n\t- ${expectedNote.note}\n- [ ] Task 2"

        val actualString = createFileContents(existingContents, listOf(expectedTask))
        actualString.orNull() shouldBe expectedString
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
