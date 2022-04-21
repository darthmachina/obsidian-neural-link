package neurallink.core.service

import neurallink.core.model.DataviewField
import neurallink.core.model.TaskId

sealed interface Problem

class UnknownProblem : Problem
data class TaskNotFoundProblem(val taskId: TaskId) : Problem
class StatusTagNotFound() : Problem
data class MissingDataviewField(val taskId: TaskId, val field: DataviewField) : Problem
