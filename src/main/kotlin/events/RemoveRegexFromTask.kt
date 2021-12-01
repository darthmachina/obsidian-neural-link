package events

import NeuralLinkPlugin

class RemoveRegexFromTask(val plugin: NeuralLinkPlugin) : TaskProcessor {
    override fun processTask(task: String) : String {
        val removeRegex = plugin.settings.taskRemoveRegex
        return task.replace(removeRegex.toRegex(), "")
    }
}