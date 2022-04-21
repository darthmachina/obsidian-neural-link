package neurallink.core.store

import arrow.core.None
import arrow.core.Some
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import neurallink.core.model.StatusTag
import neurallink.core.model.Task
import neurallink.core.model.TaskConstants
import neurallink.core.service.*

/**
 * Marks a task as complete.
 *
 * TODO: split out the new property logic into separate functions, like repeatTask()
 */
fun completeTask(
    task: Task,
    subtaskChoice: IncompleteSubtaskChoice,
    columns: Collection<StatusTag>
) : Task {
    val original = task.original ?: task.deepCopy()
    val repeatTask = repeatTask(task)

    return task.copy(
        before = when(repeatTask) {
            is Some -> repeatTask.value
            is None -> null
        },
        completed = true,
        subtasks = completedTaskSubtasks(task.subtasks, subtaskChoice),
        tags = filterTags(task.tags) { tag -> tag !in columns.map { it.tag } },
        dataviewFields = task.dataviewFields.removeKeys(
            TaskConstants.TASK_ORDER_PROPERTY,
            TaskConstants.TASK_REPEAT_PROPERTY
        ),
        original = original
    )
}

/**
 * Extension function to deepCopy a Task.
 */
@OptIn(ExperimentalSerializationApi::class)
fun Task.deepCopy(): Task {
    val bytes = Cbor.encodeToByteArray(this)
    return Cbor.decodeFromByteArray(bytes)
}
