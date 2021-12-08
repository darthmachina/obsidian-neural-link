package processor

import NeuralLinkState
import service.ModifiedTask

class RemoveRegexFromTask(val state: NeuralLinkState) : TaskProcessor {
    override fun processTask(task: ModifiedTask): ModifiedTask {
        val removeRegex = state.settings.taskRemoveRegex
        task.original = task.original.replace(removeRegex.toRegex(), "")
        return task
    }

    override fun getPriority(): Int {
        return 99
    }
}
