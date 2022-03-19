package store

import Plugin
import model.StatusTag
import model.Task
import model.TaskModel
import service.SettingsService

interface Action

data class VaultLoaded(val newTaskModel: TaskModel) : Action
data class TaskStatusChanged(val taskId: String, val newStatus: String, val beforeTask: String?) : Action
data class ModifyFileTasks(val file: String, val fileTasks: List<Task>) : Action
data class TaskCompleted(val taskId: String) : Action
data class SubtaskCompleted(val taskId: String, val subtaskId: String, val complete: Boolean) : Action
data class UpdateSettings(
    val plugin: Plugin,
    val settingsService: SettingsService,
    var taskRemoveRegex: String? = null,
    var columnTags: List<StatusTag>? = null
) : Action
