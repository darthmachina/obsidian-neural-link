package processor

import ModifiedTask
import NeuralLinkState
import Task
import service.TaskService

class RepeatingProcessor(state: NeuralLinkState, private val taskService: TaskService) : TaskProcessor {
    override fun processTask(task: ModifiedTask): Boolean {
        console.log("RepeatingProcessor, checking", task.original)
        if (taskService.isTaskRepeating(task.original)) {
            val newTask = taskService.getNextRepeatingTask(task.original)
            markIncomplete(newTask)
            console.log("New task: $newTask")
            task.before.add(newTask)
            task.original.dataviewFields.remove("repeat")
            task.modified = true
            console.log("RepeatingProcessor, modified task: ${task.modified}", task)
        }
        return task.modified
    }

    override fun getPriority(): Int {
        return 1
    }

    /**
     * Recursive method to mark all subtasks as incomplete.
     *
     * @param task The Task to process.
     */
    private fun markIncomplete(task: Task) {
        task.completed = false
        task.completedDate = null
        task.subtasks.forEach { subtask -> markIncomplete(subtask)}
    }
}
