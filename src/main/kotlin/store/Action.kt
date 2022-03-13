package store

import NeuralLinkPluginSettings
import TFile
import model.TaskModel

open interface Action

data class VaultLoaded(val newTaskModel: TaskModel) : Action
data class TaskStatusChanged(val taskId: String, val newStatus: String) : Action
data class ModifyFileTasks(val file: TFile) : Action
class TaskCompleted : Action
data class UpdateSettings(val newSettings: NeuralLinkPluginSettings) : Action