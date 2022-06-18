package neurallink.core.service.kanban

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.core.getOrElse
import arrow.core.toOption
import mu.KotlinLogging
import neurallink.core.model.DataviewField
import neurallink.core.model.DataviewValue
import neurallink.core.model.StatusTag
import neurallink.core.model.Task
import neurallink.core.model.TaskConstants
import neurallink.core.model.TaskId
import neurallink.core.model.toDataviewMap
import neurallink.core.service.BeforeTaskDoesNotExist
import neurallink.core.service.deepCopy
import neurallink.core.service.taskComparator
import neurallink.core.store.ReducerUtils

private val logger = KotlinLogging.logger("TaskOrderFunctions")

/**
 * Finds the max `pos` value in the given list. This list would usually already be sorted to
 * only include tasks for a single StatusTag.
 *
 * @return The max position value in the task list, using 0.0 when one doesn't exist.
 */
fun findMaxPositionInStatusTasks(tasks: List<Task>) : Double {
    return tasks
        .maxOf { task ->
            task.dataviewFields.valueForField(
                DataviewField(TaskConstants.TASK_ORDER_PROPERTY)
            ).getOrElse { DataviewValue(0.0) }.asDouble()
        }
}

/**
 * Finds the position between a task and the task before it.
 *
 * @return a Double halfway between beforeTaskId and the task before that in the task list
 */
fun findPositionBeforeTask(tasks: List<Task>, beforeTaskId: TaskId) : Either<BeforeTaskDoesNotExist, Double> {
    return tasks.find { it.id == beforeTaskId }
        .toOption()
        .map { beforeTask ->
            Pair( // beforeTaskPosition, beforeTaskIndex
                beforeTask
                    .dataviewFields
                    .valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY))
                    .getOrElse { DataviewValue(0.0) }
                    .asDouble(),
                tasks.indexOf(beforeTask)
            )
        }
        .map { beforeTaskPair ->
            when (beforeTaskPair.second) {
                0 -> Either.Right(beforeTaskPair.first / 2)
                else -> {
                    Either.Right((beforeTaskPair.first +
                            tasks[beforeTaskPair.second - 1]
                                .dataviewFields
                                .valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY))
                                .getOrElse { DataviewValue(0.0) }
                                .asDouble()
                            ) / 2)
                }
            }
        }
        .getOrElse {
            Either.Left(BeforeTaskDoesNotExist("Before task not found with id $beforeTaskId"))
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
fun findPosition(tasks: List<Task>, status: StatusTag, beforeTaskId: TaskId? = null) : Either<BeforeTaskDoesNotExist, Double> {
    logger.debug { "findPosition()" }

    return tasks
        .filter { task -> task.tags.contains(status.tag) }
        .let { filteredTasks ->
            when (filteredTasks.size) {
                0 -> Either.Right(1.0)
                else -> {
                    when (beforeTaskId.toOption()) {
                        is None -> Either.Right(findMaxPositionInStatusTasks(filteredTasks) + 1.0)
                        is Some -> findPositionBeforeTask(filteredTasks, beforeTaskId!!)
                    }
                }
            }
        }
}

/**
 * Finds the `pos` value that puts a task at the end of the given list
 * filtered to only include those tags tagged with the given StatusTag.
 */
fun findEndPosition(tasks: List<Task>, status: StatusTag) : Double {
    return findMaxPositionInStatusTasks(
        tasks
            .filter { task -> task.tags.contains(status.tag) }
    ) + 1.0
}

/**
 * Adds TaskConstants.TASK_ORDER_PROPERTY to each task in the list if it's not already set.
 */
fun addOrderToListItemsIfNeeded(tasks: List<Task>) : List<Task> {
    logger.debug { "addOrderToListItems()" }
    // TODO Find an FP way handle maxPosition
    var maxPosition = 1.0
    return tasks
        .sortedWith(taskComparator)
        .map { task ->
            if (!task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_ORDER_PROPERTY))) {
                updateTaskOrder(task, ++maxPosition)
            } else {
                maxPosition = task.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).getOrElse { DataviewValue(0.0) }.asDouble()
                task
            }
        }
}

/**
 * Updates the task order for a task if required (order is null or already set to the given value), saving the
 * original before making the change.
 */
fun updateTaskOrder(task: Task, position: Double): Task {
    logger.debug { "updateTaskOrder(): ${task.description}" }
    logger.trace { " - $task, $position" }
    val taskOrder = task.dataviewFields[DataviewField(TaskConstants.TASK_ORDER_PROPERTY)]
    if (taskOrder == null || taskOrder.asDouble() != position) {
        logger.debug { " - order requires updating" }
        return task.copy(
            original = task.original ?: task.deepCopy(),
            dataviewFields = task.dataviewFields
                .plus(
                    DataviewField(TaskConstants.TASK_ORDER_PROPERTY) to
                            DataviewValue(position)).toDataviewMap()
        )
    }
    return task
}
