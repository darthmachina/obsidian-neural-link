package processor

import NeuralLinkState
import service.TaskService

class RecurringProcessor(state: NeuralLinkState, private val taskService: TaskService) : TaskProcessor {
    override fun processTask(task: String): String {
        val dueDate = taskService.getDueDateFromTask(task)
        console.log("Task due: ${dueDate?.toDateString()}")
        return task
    }
}
