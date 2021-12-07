package processor

import NeuralLinkState
import service.TaskService

class RecurringProcessor(state: NeuralLinkState, private val taskService: TaskService) : TaskProcessor {
    override fun processTask(task: String, fileContents : List<String>, line: Int): String {
        val newTask = taskService.getNextRecurringTask(task)
        console.log("New task: $newTask")
        return task
    }
}
