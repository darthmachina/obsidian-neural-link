package store

import NeuralLinkPluginSettings
import model.TaskModel

open interface Action

data class VaultLoaded(val newTaskModel: TaskModel) : Action
class ModifyFileTasks : Action
class TaskCompleted : Action
data class UpdateSettings(val newSettings: NeuralLinkPluginSettings) : Action