package store

import Plugin
import neurallink.core.model.DataviewPair
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag
import neurallink.core.model.Task
import neurallink.core.model.TaskFile
import neurallink.core.model.TaskId
import service.RepeatingTaskService
import service.SettingsService

interface Action

enum class IncompleteSubtaskChoice {
    NOTHING,
    COMPLETE,
    DELETE
}

sealed class FilterValue<T>(open val filterValue : T? = null)
class NoneFilterValue() : FilterValue<Any>()
class TagFilterValue(override val filterValue: Tag) : FilterValue<Tag>(filterValue)
class FileFilterValue(override val filterValue: TaskFile) : FilterValue<TaskFile>(filterValue)
class DataviewFilterValue(override val filterValue: DataviewPair) : FilterValue<DataviewPair>(filterValue)
class FutureDateFilterValue(override val filterValue: Boolean) : FilterValue<Boolean>(filterValue)

data class VaultLoaded(val tasks: List<Task>) : Action
data class TaskMoved(val taskId: TaskId, val newStatus: StatusTag, val beforeTask: TaskId? = null) : Action
data class MoveToTop(val taskd: TaskId) : Action
data class ModifyFileTasks(val file: TaskFile, val fileTasks: List<Task>, val repeatingTaskService: RepeatingTaskService) : Action
data class TaskCompleted(
    val taskId: TaskId,
    val repeatingTaskService: RepeatingTaskService,
    val subtaskChoice: IncompleteSubtaskChoice = IncompleteSubtaskChoice.NOTHING
) : Action
data class SubtaskCompleted(val taskId: TaskId, val subtaskId: TaskId, val complete: Boolean) : Action
data class RepeatTask(val taskId: TaskId, val repeatingTaskService: RepeatingTaskService) : Action
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
