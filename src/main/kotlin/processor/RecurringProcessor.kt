package processor

import NeuralLinkState
import service.ModifiedTask
import service.TaskService

class RecurringProcessor(state: NeuralLinkState, private val taskService: TaskService) : TaskProcessor {
    override fun processTask(task: ModifiedTask): ModifiedTask {
        if (taskService.isTaskRepeating(task.original)) {
            val newTask = taskService.getNextRepeatingTask(task.original)
            console.log("New task: $newTask")
            task.before.add(newTask)
            task.original = taskService.removeRepeatText(task.original)
        }
        return task
    }

    override fun getPriority(): Int {
        return 1
    }
}
