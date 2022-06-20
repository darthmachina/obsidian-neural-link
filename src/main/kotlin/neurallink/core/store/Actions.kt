package neurallink.core.store

import Plugin
import mu.KotlinLoggingLevel
import neurallink.core.model.*

sealed interface Action

enum class IncompleteSubtaskChoice {
    NOTHING,
    COMPLETE,
    DELETE
}

sealed class FilterValue<T>(open val filterValue : T? = null)
class NoneFilterValue() : FilterValue<Any>()
class TagFilterValue(override val filterValue: Tag) : FilterValue<Tag>(filterValue)
class FileFilterValue(override val filterValue: TaskFile) : FilterValue<TaskFile>(filterValue)
class DataviewFilterValue(override val filterValue: DataviewPair<out Comparable<*>>) : FilterValue<DataviewPair<out Comparable<*>>>(filterValue)
class FutureDateFilterValue(override val filterValue: Boolean) : FilterValue<Boolean>(filterValue)

data class VaultLoaded(val tasks: List<Task>) : Action
data class FileDeleted(val file: TaskFile) : Action
data class TaskMoved(val taskId: TaskId, val newStatus: StatusTag, val beforeTask: TaskId? = null) : Action
data class MoveToTop(val taskd: TaskId) : Action
data class ModifyFileTasks(val file: TaskFile, val fileTasks: List<Task>) : Action
data class TaskCompleted(
    val taskId: TaskId,
    val subtaskChoice: IncompleteSubtaskChoice = IncompleteSubtaskChoice.NOTHING
) : Action
data class SubtaskCompleted(val taskId: TaskId, val subtaskId: TaskId, val complete: Boolean) : Action
data class RepeatTask(val taskId: TaskId) : Action
data class FilterByTag(val tag: String?) : Action
data class FilterByFile(val file: String?) : Action
data class FilterByDataviewValue(val value: String?) : Action
data class FilterFutureDate(val filter: Boolean) : Action
data class UpdateSettings(
    val plugin: Plugin,
    val taskRemoveRegex: String? = null,
    val columnTags: List<StatusTag>? = null,
    val tagColors: Map<Tag,String>? = null,
    val logLevel: KotlinLoggingLevel? = null
) : Action
