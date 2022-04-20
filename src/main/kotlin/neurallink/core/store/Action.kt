package neurallink.core.store

import Plugin
import neurallink.core.model.StatusTag
import neurallink.core.model.Task
import service.SettingsService

interface Action

enum class IncompleteSubtaskChoice {
    NOTHING,
    COMPLETE,
    DELETE
}

data class VaultLoaded(val tasks: List<Task>) : Action
data class TaskMoved(val taskId: String, val newStatus: String, val beforeTask: String? = null) : Action
data class MoveToTop(val taskd: String) : Action
data class ModifyFileTasks(val file: String, val fileTasks: List<Task>, val repeatingTaskService: RepeatingTaskService) :
    Action
data class TaskCompleted(
    val taskId: String,
    val repeatingTaskService: RepeatingTaskService,
    val subtaskChoice: IncompleteSubtaskChoice = IncompleteSubtaskChoice.NOTHING
) : Action
data class SubtaskCompleted(val taskId: String, val subtaskId: String, val complete: Boolean) : Action
data class RepeatTask(val taskId: String, val repeatingTaskService: RepeatingTaskService) : Action
data class FilterByTag(val tag: String?) : Action
data class FilterByFile(val file: String?) : Action
data class FilterByDataviewValue(val value: String?) : Action
data class FilterFutureDate(val filter: Boolean) : Action
data class UpdateSettings(
    val plugin: Plugin,
    val settingsService: SettingsService,
    var taskRemoveRegex: String? = null,
    var columnTags: List<StatusTag>? = null,
    var tagColors: Map<String,String>? = null
) : Action
