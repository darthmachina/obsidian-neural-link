package processor

import ModifiedTask
import NeuralLinkState
import service.TaskService

class RemoveRegexFromTask(private val state: NeuralLinkState, private val taskService: TaskService) : TaskProcessor {
    /**
     * Creates a copy of the given task, recreating the Task from the original text
     * after it has been run through the RegEx and returns the new Task
     */
    override fun processTask(task: ModifiedTask): ModifiedTask {
        val removeRegex = state.settings.taskRemoveRegex

        val updatedDescription = task.original.full.replace(removeRegex.toRegex(), "")
        if (updatedDescription != task.original.full) {
            task.original = taskService.createTask(updatedDescription)
            task.modified = true
        }
        return task
    }

    override fun getPriority(): Int {
        return 99
    }
}
