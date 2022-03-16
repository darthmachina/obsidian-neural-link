package store

import NeuralLinkPluginSettings
import TFile
import Vault
import model.Task
import model.TaskModel

open interface Action

data class VaultLoaded(val newTaskModel: TaskModel) : Action
data class TaskStatusChanged(val taskId: String, val newStatus: String, val beforeTask: String?) : Action
data class ModifyFileTasks(val file: String, val fileTasks: List<Task>) : Action
class TaskCompleted : Action
data class UpdateSettings(val newSettings: NeuralLinkPluginSettings) : Action
