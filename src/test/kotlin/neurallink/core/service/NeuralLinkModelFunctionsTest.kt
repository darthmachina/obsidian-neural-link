package neurallink.core.service

import arrow.core.filterOrElse
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import neurallink.core.model.TaskFile
import neurallink.test.TestFactory

class NeuralLinkModelFunctionsTest : StringSpec({
    "removeTasksForFile() removes just tasks for a file" {
        val expectedSize = 5
        val fileBeingRemoved = "testfile.md"
        val taskToDelete = TestFactory.createTask(file = fileBeingRemoved)
        val tasks = TestFactory.createTasks(expectedSize) + taskToDelete

        val actualList = removeTasksForFile(tasks, TaskFile(fileBeingRemoved)).shouldBeRight()
        actualList shouldNotContain taskToDelete
        actualList shouldHaveSize expectedSize
    }
})