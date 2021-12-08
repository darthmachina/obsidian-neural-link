package processor

import NeuralLinkState
import service.TaskService

class RecurringProcessor(state: NeuralLinkState, private val taskService: TaskService) : TaskProcessor {
    override fun processTask(task: String): String {
        return if (taskService.isTaskRecurring(task)) {
            val newTask = taskService.getNextRecurringTask(task)
            console.log("New task: $newTask")
            // Return the newTask above the current task (string split by a newline)
            "$newTask\n${taskService.removeRecurText(task)}"
        } else {
            task
        }
    }

    override fun getPriority(): Int {
        return 1
    }
}
