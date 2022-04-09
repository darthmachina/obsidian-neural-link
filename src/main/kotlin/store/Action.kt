package store

import Plugin
import model.StatusTag
import model.Task
import service.RepeatingTaskService
import service.SettingsService

interface Action

data class VaultLoaded(val tasks: List<Task>) : Action
data class TaskMoved(val taskId: String, val newStatus: String, val beforeTask: String?) : Action
data class ModifyFileTasks(val file: String, val fileTasks: List<Task>, val repeatingTaskService: RepeatingTaskService) : Action
data class TaskCompleted(val taskId: String, val repeatingTaskService: RepeatingTaskService) : Action
data class SubtaskCompleted(val taskId: String, val subtaskId: String, val complete: Boolean) : Action
data class RepeatTask(val taskId: String, val repeatingTaskService: RepeatingTaskService) : Action
data class FilterByTag(val tag: String?) : Action
data class FilterByFile(val file: String?) : Action
data class UpdateSettings(
    val plugin: Plugin,
    val settingsService: SettingsService,
    var taskRemoveRegex: String? = null,
    var columnTags: List<StatusTag>? = null
) : Action
