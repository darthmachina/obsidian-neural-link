package neurallink.core.service.kanban

import arrow.core.*
import neurallink.core.model.*
import neurallink.core.service.BeforeTaskDoesNotExist
import neurallink.core.service.NoStatusTagOnTaskWarning
import neurallink.core.service.taskComparator
import neurallink.core.service.taskDateComparator
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
        .mapNotNull { task ->
            getStatusTagFromTask(task, statusTags)
                .map {
                    task to it
                }.orNull()
        }
        .groupBy { it.second }
        .mapValues { entry -> entry.value.map { it.first } }
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
