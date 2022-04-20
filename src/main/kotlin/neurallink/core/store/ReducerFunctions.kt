package neurallink.core.store

import arrow.core.None
import arrow.core.Some
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import model.StatusTag
import neurallink.core.model.Task
import neurallink.core.model.TaskConstants
import neurallink.core.service.repeatTask

enum class IncompleteSubtaskChoice {
    NOTHING,
    COMPLETE,
    DELETE
}

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
    val newSubtasks = when (subtaskChoice) {
        IncompleteSubtaskChoice.DELETE -> task.subtasks.filter { it.completed }
        IncompleteSubtaskChoice.COMPLETE -> {
            task.subtasks.map {
                if (it.completed) {
                    it
                } else {
                    it.copy(completed = true)
                }
            }
        }
        else -> task.subtasks
    }
    val newFields = task.dataviewFields.filter {
        it.key != TaskConstants.TASK_ORDER_PROPERTY &&
            it.key != TaskConstants.TASK_REPEAT_PROPERTY
    }
    val newTags = task.tags.filter { tag -> tag !in columns.map { it.tag } }.toSet()

    return task.copy(
        before = when(repeatTask) {
            is Some -> repeatTask.value
            is None -> null
        },
        completed = true,
        subtasks = newSubtasks,
        tags = newTags,
        dataviewFields = newFields,
        original = original
    )
}

fun setTaskOrder(fields: Map<String,String>, order: Int) : Map<String,String> {
    return if (fields.containsKey(TaskConstants.TASK_ORDER_PROPERTY) && fields[TaskConstants.TASK_ORDER_PROPERTY] == order.toString()) {
        fields
    } else {
        fields
            .toMutableMap() // Functions as a copy()
            .apply {
                this[TaskConstants.TASK_ORDER_PROPERTY] = order.toString()
            }
    }
}

/**
 * Extension function to deepCopy a Task.
 */
@OptIn(ExperimentalSerializationApi::class)
fun Task.deepCopy(): Task {
    val bytes = Cbor.encodeToByteArray(this)
    return Cbor.decodeFromByteArray(bytes)
}
