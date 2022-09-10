package neurallink.core.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import neurallink.core.model.Description
import neurallink.core.model.TaskFile
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
        fileTasks[0] = fileTasks[0].copy(description = Description("Changed"))

        val actualTasks = changedTasks("testfile.md", fileTasks, vaultTasks)
        actualTasks shouldHaveSize 1
        actualTasks[0].description.value shouldBe "Changed"
    }
})