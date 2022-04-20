package neurallink.view

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import neurallink.core.model.DataviewMap
import neurallink.core.model.DataviewValue
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag
import neurallink.core.model.Task
import neurallink.core.model.TaskConstants
import neurallink.core.model.TaskId
import neurallink.core.service.filterByTag
import neurallink.core.service.findById
import neurallink.core.service.sortByTaskOrder

/**
 * Gets the StatusTag attached to a Task.
 */
fun findStatusTag(tags: Collection<Tag>, kanbanKeys: Collection<StatusTag>) : Either<String, StatusTag> {
    return if (tags.any { tag -> tag in kanbanKeys.map { it.tag } }) {
        Either.Right(kanbanKeys.find { tags.contains(it.tag) }!!)
    } else {
        Either.Left("No status tag found")
    }
}

fun List<Task>.filterTasksByStatusTag(tag: StatusTag): List<Task> {
    return this.filter { task -> task.tags.contains(tag.tag) }
}

/**
 * Gets the first task position.
 *
 * @param tasks List of tasks for a single StatusTag
 * @return 1.0 if the list is empty, otherwise the `pos` value
 */
fun firstTaskPosition(tasks: List<Task>) : Double {
    return if (tasks.isEmpty()) {
        1.0
    } else {
        tasks
            .sortByTaskOrder()
            .first()
            .dataviewFields.valueForField(TaskConstants.TASK_ORDER_PROPERTY)
            .flatMap { it.asDouble() }
            .getOrElse { 1.0 }
    }
}

/**
 * Gets the first task position.
 *
 * @param tasks List of tasks for a single StatusTag
 * @return 1.0 if the list is empty, otherwise the `pos` value
 */
fun lastTaskPosition(tasks: List<Task>) : Double {
    return if (tasks.isEmpty()) {
        1.0
    } else {
        tasks
            .sortByTaskOrder()
            .last()
            .dataviewFields.valueForField(TaskConstants.TASK_ORDER_PROPERTY)
            .flatMap { it.asDouble() }
            .getOrElse { 1.0 }
    }
}

/**
 * Upserts the `pos` field for a Task.
 */
fun setTaskOrder(fields: DataviewMap, order: Double) : DataviewMap {
    return if (fields.containsKey(TaskConstants.TASK_ORDER_PROPERTY) && fields.valueForField(TaskConstants.TASK_ORDER_PROPERTY) == order) {
        fields
    } else {
        fields
            .copy()
            .apply {
                this[TaskConstants.TASK_ORDER_PROPERTY] = DataviewValue(order)
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
fun findPosition(tasks: List<Task>, status: Tag, beforeTaskId: TaskId? = null) : Either<String,Double> {
    console.log("KanbanFunctions.findPosition()")
    return if (tasks.none { task -> task.tags.contains(status) }) {
        Either.Right(1.0)
    } else if (beforeTaskId == null) {
        Either.Right(lastTaskPosition(tasks) + 1.0)
    } else {
        val statusTasks = tasks.filterByTag(status)
        val beforeTask = statusTasks.findById(beforeTaskId)
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
