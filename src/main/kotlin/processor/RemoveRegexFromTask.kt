package processor

import ModifiedTask
import NeuralLinkState
import service.TaskService

class RemoveRegexFromTask(val state: NeuralLinkState, val taskService: TaskService) : TaskProcessor {
    /**
     * Creates a copy of the given task, recreating the Task from the original text
     * after it has been run through the RegEx and returns the new Task
     */
    override fun processTask(task: ModifiedTask): ModifiedTask {
        val removeRegex = state.settings.taskRemoveRegex

        val updatedDescription = task.original.full.replace(removeRegex.toRegex(), "")
        task.original = taskService.createTask(updatedDescription)
        return task
    }

    override fun getPriority(): Int {
        return 99
    }
}
