package neurallink.core.service

import arrow.core.*
import neurallink.core.model.*
import neurallink.core.store.ReducerUtils

/**
 * Create a map of StatusTag -> List<Task> for any task that has a StatusTag on it
 */
fun createKanbanMap(tasks: List<Task>, statusTags: List<StatusTag>) : Map<StatusTag,List<Task>> {
    console.log("Reducers.ReducerUtils.createKanbanMap()")
    return tasks
        .filter { task ->
            task.tags.any { tag ->
                tag in statusTags.map { it.tag }
            }
        }
        .groupBy { task -> getStatusTagFromTask(task, statusTags).orNull() }
        .filterKeys { it != null }
        .plus(
            statusTags
                .minus(getAllStatusTagsOnTasks(tasks, statusTags))
                .map { statusTag -> Pair(statusTag, emptyList()) }
        )
        .mapValues {
            it.value
                .sortedWith(
                    if (it.key.dateSort)
                        taskDateComparator
                    else
                        taskComparator
                )
        }
        .mapValues { entry ->
            if (!entry.key.dateSort) {
                addOrderToListItemsIfNeeded(entry.value)
            } else {
                entry.value
            }
        }
}

/**
 * Adds TaskConstants.TASK_ORDER_PROPERTY to each task in the list if it's not already set.
 */
private fun addOrderToListItemsIfNeeded(tasks: List<Task>) : List<Task> {
//            console.log("addOrderToListItems()")
    // TODO Find an FP way handle maxPosition
    var maxPosition = 1.0
    return tasks
        .sortedWith(taskComparator)
        .map { task ->
            if (!task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_ORDER_PROPERTY))) {
                ReducerUtils.updateTaskOrder(task, maxPosition++)
            } else {
                maxPosition = task.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).getOrElse { DataviewValue(0.0) }.asDouble()
                task
            }
        }
}

fun getAllStatusTagsOnTasks(tasks: List<Task>, statusTags: List<StatusTag>) : Set<StatusTag> {
//            console.log("getAllStatusTagsOnTasks()", tasks, statusTags)
    return tasks
        .asSequence()
        .map { task -> task.tags }
        .flatten()
        .distinct()
        .filter { tag -> tag in statusTags.map{ it.tag } }
        .map { tag -> statusTags.find { statusTag -> statusTag.tag == tag }!! }
        .toSet()
}

fun getStatusTagFromTask(task: Task, kanbanKeys: Collection<StatusTag>): Either<NoStatusTagOnTaskWarning,StatusTag> {
//            console.log("Reducers.ReducerUtils.getStatusTagFromTask()", task)
    return kanbanKeys
        .filter { statusTag -> task.tags.contains(statusTag.tag) }
        .let {
            when (it.size) {
                0 -> Either.Left(NoStatusTagOnTaskWarning("There is no status tag on task '${task.description}"))
                1 -> it[0].right()
                else -> {
                    console.log(" - WARN: More than one status column is on the task, using the first: ", it[0])
                    it[0].right()
                }
            }
        }
}

fun filterStatusTags(tags: Set<Tag>, columns: Collection<StatusTag>) : Set<Tag> {
    return tags.filter { tag -> tag !in columns.map { it.tag } }.toSet()
}

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
                DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).getOrElse { DataviewValue(0.0) }.asDouble()
        }
}

/**
 * Finds the position between a task and the task before it.
 *
 * @return a Double halfway between beforeTaskId and the task before that in the task list
 */
fun findPositionBeforeTask(tasks: List<Task>, beforeTaskId: TaskId) : Either<BeforeTaskDoesNotExist,Double> {
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
fun findPosition(tasks: List<Task>, status: StatusTag, beforeTaskId: TaskId? = null) : Either<BeforeTaskDoesNotExist,Double> {
    console.log("ReducerUtils.findPosition()")

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
