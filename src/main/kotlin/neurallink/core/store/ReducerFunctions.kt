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

fun setTaskOrder(fields: Map<String,String>, order: Double) : Map<String,String> {
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
 * Finds the position for a task, calculated given the following scenarios:
 *
 * 1. No tasks for the status, returns 1.0 (to leave room before it for other cards)
 * 2. No beforeTaskId given, returns the max position value + 1 to put it at the end
 * 3. beforeTaskId given, find that task and the one before it and returns a value in the middle of the two pos values
 *  - If beforeTask is the first in the list just return its position divided by 2
 */
fun findPosition(tasks: List<Task>, status: String, beforeTaskId: String? = null) : Double {
    console.log("ReducerUtils.findPosition()")
    return if (tasks.none { task -> task.tags.contains(status) }) {
//                console.log(" - list is empty, returning 1.0")
        1.0
    } else if (beforeTaskId == null) {
//                console.log(" - no beforeTaskId, adding to end of list")
        (tasks
            .filter { task -> task.tags.contains(status) }
            .sortedWith(compareBy(nullsLast()) { task -> task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toDouble() })
            .last()
            .dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]!!.toDouble()) + 1.0
    } else {
//                console.log(" - beforeTaskId set, finding new position")
        val statusTasks = tasks
            .filter { task -> task.tags.contains(status) }
            .sortedWith(compareBy(nullsLast()) { task -> task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toDouble() })
        val beforeTask = statusTasks.find { it.id == beforeTaskId }
            ?: throw IllegalStateException("beforeTask not found for id $beforeTaskId")
        val beforeTaskPosition = beforeTask.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toDouble()
            ?: throw IllegalStateException("beforeTask does not have a position property")
        val beforeTaskIndex = statusTasks.indexOf(beforeTask)
        // Returns new position
        if (beforeTaskIndex == 0) {
            beforeTaskPosition / 2
        } else {
            val beforeBeforeTask = statusTasks[beforeTaskIndex - 1]
            val beforeBeforeTaskPosition = beforeBeforeTask.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toDouble()
                ?: throw IllegalStateException("beforeBeforeTask does not have a position property")
            (beforeTaskPosition + beforeBeforeTaskPosition) / 2
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
