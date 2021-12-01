package processor

import NeuralLinkState

class RemoveRegexFromTask(val state: NeuralLinkState) : TaskProcessor {
    override fun processTask(task: String) : String {
        val removeRegex = state.settings.taskRemoveRegex
        val newText = task.replace(removeRegex.toRegex(), "")
        console.log("RemoveRegexFromTask, before and after:", task, newText)
        return newText
    }
}
