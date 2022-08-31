package neurallink.core.service

import arrow.core.Either
import mu.KotlinLogging
import neurallink.core.model.NeuralLinkModel
import neurallink.core.model.*
import neurallink.core.service.kanban.filterOutStatusTags
import neurallink.core.store.IncompleteSubtaskChoice

private val logger = KotlinLogging.logger("TaskFunctions")

fun Task.deepCopy(): Task {
    logger.debug { "deepCopy()" }
    return this.copy(
        tags = tags.map { tag -> tag.copy() }.toSet(),
        dataviewFields = dataviewFields.copy(),
        subtasks = subtasks.map { subtask -> subtask.deepCopy() },
        notes = notes.map { note -> note.copy() }
    )
}

fun subtasksForCompletedTask(subtasks: List<Task>, subtaskChoice: IncompleteSubtaskChoice) : List<Task> {
    logger.info { "subtasksForCompletedTask(), choice: ${subtaskChoice.name}" }
    return when (subtaskChoice) {
        IncompleteSubtaskChoice.DELETE -> {
            subtasks.filter { it.completed }
        }
        IncompleteSubtaskChoice.COMPLETE -> {
            subtasks.map {
                if (it.completed) it else it.copy(completed = true)
            }
        }
        IncompleteSubtaskChoice.NOTHING -> subtasks
    }
}

fun filterDataviewKeys(fields: DataviewMap, vararg keys: DataviewField) : DataviewMap {
    return fields
        .filterKeys { key ->
            key !in keys
        }
        .toDataviewMap()
}

fun checkAndCreateRepeatingTask(task: Task) : Either<NeuralLinkError, Task> {
    return if (isTaskRepeating(task))
        getNextRepeatingTask(task)
    else
        Either.Left(NotARepeatingTaskWarning("Task isn't a repeating task"))
}

/**
 * Marks a task as complete.
 *
 * Also performs the following on the task:
 *  1. Modifies the subtask list according to the IncompleteSubtaskChoice
 *  2.
 */
fun completeTask(
    task: Task,
    subtaskChoice: IncompleteSubtaskChoice,
    columns: Collection<StatusTag>
) : Task {
    logger.debug { "completeTask()" }
    return task.copy(
        original = task.original ?: task.deepCopy(),
        completed = true,
        subtasks = subtasksForCompletedTask(task.subtasks, subtaskChoice),
        dataviewFields = filterDataviewKeys(
            task.dataviewFields,
            DataviewField(TaskConstants.TASK_ORDER_PROPERTY),
            DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)),
        tags = filterOutStatusTags(task.tags, columns),
        before = checkAndCreateRepeatingTask(task)
            .mapLeft {
                if (it.isError) logger.error { "Cannot create repeating task: $it" }
            }.orNull()
    )
}

/**
 * Returns a list of Tasks from a file that have changed from within the store
 */
fun changedTasks(file: String, fileTasks: List<Task>, store: NeuralLinkModel) : List<Task> {
    // Take the fileTasks list and subtrack any that are equal to what is already in the store
    val storeFileTasks = store.tasks.filter { it.file.value == file }
    if (storeFileTasks.isEmpty()) return emptyList()

    logger.debug { "ReducerUtils.changedTasks(): $fileTasks, $storeFileTasks" }
    return fileTasks.minus(storeFileTasks.toSet())
}
