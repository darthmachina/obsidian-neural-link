package neurallink.core.service.kanban

import arrow.core.*
import mu.KotlinLogging
import neurallink.core.model.*
import neurallink.core.service.NoStatusTagOnTaskWarning
import neurallink.core.service.taskComparator
import neurallink.core.service.taskContainsAnyStatusTag
import neurallink.core.service.taskDateComparator

private val logger = KotlinLogging.logger("KanbanFunctions")

/**
 * Create a map of StatusTag -> List<Task> for any task that has a StatusTag on it
 */
fun createKanbanMap(tasks: List<Task>, statusTags: List<StatusTag>) : Map<StatusTag,List<Task>> {
    logger.debug { "createKanbanMap()" }
    return tasks
        .filter { task ->
            taskContainsAnyStatusTag(task, statusTags)
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
}

fun getAllStatusTagsOnTasks(tasks: List<Task>, statusTags: List<StatusTag>) : Set<StatusTag> {
    logger.debug { "getAllStatusTagsOnTasks()" }
    logger.trace { " - $tasks, $statusTags" }
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
    logger.debug { "getStatusTagFromTask()" }
    logger.trace { " - $task" }
    return kanbanKeys
        .filter { statusTag -> task.tags.contains(statusTag.tag) }
        .let {
            when (it.size) {
                0 -> Either.Left(NoStatusTagOnTaskWarning("There is no status tag on task '${task.description}"))
                1 -> it[0].right()
                else -> {
                    logger.warn { " - WARN: More than one status column is on the task, using the first: ${it[0]}" }
                    it[0].right()
                }
            }
        }
}

fun filterOutStatusTags(tags: Set<Tag>, columns: Collection<StatusTag>) : Set<Tag> {
    return tags.filter { tag -> tag !in columns.map { it.tag } }.toSet()
}
