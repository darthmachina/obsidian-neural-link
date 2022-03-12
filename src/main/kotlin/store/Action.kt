package store

import NeuralLinkPluginSettings
import TFile
import model.TaskModel

open interface Action

data class VaultLoaded(val newTaskModel: TaskModel) : Action
class ModifyFileTasks(val file: TFile) : Action
class TaskCompleted : Action
data class UpdateSettings(val newSettings: NeuralLinkPluginSettings) : Action