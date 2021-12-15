package processor

import ModifiedTask
import NeuralLinkState
import Task
import service.TaskService

class RemoveTagsFromTask(private val state: NeuralLinkState, private val taskService: TaskService) : TaskProcessor {
    /**
     * Removes any tags matching the user-specified RegEx in settings
     */
    override fun processTask(task: ModifiedTask): ModifiedTask {
        if (state.settings.taskRemoveRegex.isNotEmpty()) {
            val tagsRegex = Regex(state.settings.taskRemoveRegex)
            task.modified = removeTagsFromTask(task.original, tagsRegex)
        }
        return task
    }

    override fun getPriority(): Int {
        return 99
    }

    /**
     * Recursive method to remove tags from a Task and all subtasks
     *
     * @param task The Task to check
     * @param regex The regular expression to use
     * @return true if a tag was removed, false otherwise
     */
    private fun removeTagsFromTask(task: Task, regex: Regex) : Boolean {
        val removed = task.tags.removeAll { tag -> regex.containsMatchIn(tag) }
        return if (task.subtasks.size > 0) {
            val subtasksRemoved = task.subtasks.map { subtask -> removeTagsFromTask(subtask, regex) }
            removed || (true in subtasksRemoved)
        } else {
            removed
        }
    }
}
