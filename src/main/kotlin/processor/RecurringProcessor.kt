package processor

import ModifiedTask
import NeuralLinkState
import service.TaskService

class RecurringProcessor(state: NeuralLinkState, private val taskService: TaskService) : TaskProcessor {
    override fun processTask(task: ModifiedTask): ModifiedTask {
        console.log("RecurringProcessor, checking", task.original)
        if (taskService.isTaskRepeating(task.original)) {
            val newTask = taskService.getNextRepeatingTask(task.original)
            console.log("New task: $newTask")
            task.before.add(newTask)
            task.original.dataviewFields.remove("repeat")
            console.log("Dataview keys", task.original.dataviewFields.map { it.key })
            task.modified = true
        }
        return task
    }

    override fun getPriority(): Int {
        return 1
    }
}
