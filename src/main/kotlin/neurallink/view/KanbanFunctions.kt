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
import neurallink.core.service.MissingDataviewField
import neurallink.core.service.Problem
import neurallink.core.service.StatusTagNotFound
import neurallink.core.service.TaskNotFoundProblem
import neurallink.core.service.UnknownProblem
import neurallink.core.service.filterByTag
import neurallink.core.service.findById
import neurallink.core.service.sortByTaskOrder

/**
 * Gets the StatusTag attached to a Task.
 */
fun findStatusTag(tags: Collection<Tag>, kanbanKeys: Collection<StatusTag>) : Either<Problem, StatusTag> {
    return if (tags.any { tag -> tag in kanbanKeys.map { it.tag } }) {
        Either.Right(kanbanKeys.find { tags.contains(it.tag) }!!)
    } else {
        Either.Left(StatusTagNotFound())
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
    return if (fields.containsKey(TaskConstants.TASK_ORDER_PROPERTY) &&
            fields.valueForField(TaskConstants.TASK_ORDER_PROPERTY).getOrElse { false } == order) {
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
 * Gets the Task for beforeTaskId.
 * TODO Not really happy with this method; look for ways to refactor.
 *
 * @param tasks Ordered list of tasks for a single Status
 * @return A Triple with the Task, Position and List Index
 */
fun findTaskWithIndexAndPosition(tasks: List<Task>, taskId: TaskId) : Either<Problem, Triple<Task,Double,Int>> {
    val beforeTask = tasks.findById(taskId).getOrElse { null }
        ?: return Either.Left(TaskNotFoundProblem(taskId))
    val beforeTaskPosition = beforeTask.dataviewFields.valueForField(TaskConstants.TASK_ORDER_PROPERTY)
        .flatMap { it.asDouble() }.getOrElse { null }
        ?: return Either.Left(MissingDataviewField(taskId, TaskConstants.TASK_ORDER_PROPERTY))
    val beforeTaskIndex = tasks.indexOf(beforeTask)
    return Either.Right(Triple(beforeTask, beforeTaskPosition, beforeTaskIndex))

}

/**
 * Finds the position for a task, calculated given the following scenarios:
 *
 * 1. No tasks for the status, returns 1.0 (to leave room before it for other cards)
 * 2. No beforeTaskId given, returns the max position value + 1 to put it at the end
 * 3. beforeTaskId given, find that task and the one before it and returns a value in the middle of the two pos values
 *  - If beforeTask is the first in the list just return its position divided by 2
 */
fun findPosition(tasks: List<Task>, status: Tag, beforeTaskId: TaskId? = null) : Either<Problem,Double> {
    console.log("KanbanFunctions.findPosition()")
    var position: Either<Problem,Double> = Either.Left(UnknownProblem())
    if (tasks.none { task -> task.tags.contains(status) }) {
        position = Either.Right(1.0)
    } else if (beforeTaskId == null) {
        position = Either.Right(lastTaskPosition(tasks) + 1.0)
    } else {
        // TODO This whole section sucks
        val statusTasks = tasks.filterByTag(status)
        val maybeBeforeTask = findTaskWithIndexAndPosition(statusTasks, beforeTaskId)
            .flatMap {
                val (_, beforeTaskPosition, beforeTaskIndex) = it
                if (beforeTaskIndex == 0) {
                    Either.Right(beforeTaskPosition / 2)
                } else {
                    val beforeBeforeTask = statusTasks[beforeTaskIndex - 1]
                    beforeBeforeTask.dataviewFields.valueForField(TaskConstants.TASK_ORDER_PROPERTY)
                        .flatMap { it.asDouble() }
                        .fold { beforeBeforeTaskPosition ->
                            Either.Right((beforeTaskPosition + beforeBeforeTaskPosition) / 2)
                        },
                        {
                            Either.Left(MissingDataviewField(beforeBeforeTask.id, TaskConstants.TASK_ORDER_PROPERTY))
                        }
                }
            }
    }
    return position
}


