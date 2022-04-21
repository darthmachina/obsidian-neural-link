package neurallink.core.store

import Plugin
import kotlinx.datetime.LocalDate
import neurallink.core.model.DataviewPair
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag
import neurallink.core.model.Task
import neurallink.core.model.TaskFile
import neurallink.core.model.TaskId
import service.SettingsService

interface Action

enum class IncompleteSubtaskChoice {
    NOTHING,
    COMPLETE,
    DELETE
}

enum class FilterType {
    NONE,
    TAG,
    FILE,
    DATAVIEW,
    CURRENT_DATE
}

sealed class FilterValue<T>(open val filterValue : T)
class TagFilterValue(override val filterValue: Tag) : FilterValue<Tag>(filterValue)
class FileFilterValue(override val filterValue: TaskFile) : FilterValue<TaskFile>(filterValue)
class DataviewFilterValue(override val filterValue: DataviewPair) : FilterValue<DataviewPair>(filterValue)
class FutureDateFilterValue(override val filterValue: Boolean) : FilterValue<Boolean>(filterValue)

data class VaultLoaded(val tasks: List<Task>) : Action
data class TaskMoved(val taskId: TaskId, val newStatus: String, val beforeTask: TaskId? = null) : Action
data class MoveToTop(val taskId: TaskId) : Action
data class ModifyFileTasks(val file: TaskFile, val fileTasks: List<Task>) :
    Action
data class TaskCompleted(
    val taskId: TaskId,
    val subtaskChoice: IncompleteSubtaskChoice = IncompleteSubtaskChoice.NOTHING
) : Action
data class SubtaskCompleted(val taskId: TaskId, val subtaskId: TaskId, val complete: Boolean) : Action
data class RepeatTask(val taskId: TaskId) : Action
data class FilterByTag(val tag: Tag?) : Action
data class FilterByFile(val file: TaskFile?) : Action
data class FilterByDataviewValue(val value: String?) : Action
data class FilterFutureDate(val filter: Boolean) : Action
data class UpdateSettings(
    val plugin: Plugin,
    val settingsService: SettingsService,
    var taskRemoveRegex: String? = null,
    var columnTags: List<StatusTag>? = null,
    var tagColors: Map<String,String>? = null
) : Action
