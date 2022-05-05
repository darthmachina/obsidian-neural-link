package neurallink.core.service

import arrow.core.Either
import model.NeuralLinkModel
import neurallink.core.model.*
import neurallink.core.service.kanban.filterStatusTags
import neurallink.core.store.IncompleteSubtaskChoice

fun subtasksForCompletedTask(subtasks: List<Task>, subtaskChoice: IncompleteSubtaskChoice) : List<Task> {
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
    console.log("completeTask()")
    return task.copy(
        original = task.original ?: task.deepCopy(),
        completed = true,
        subtasks = subtasksForCompletedTask(task.subtasks, subtaskChoice),
        dataviewFields = filterDataviewKeys(
            task.dataviewFields,
            DataviewField(TaskConstants.TASK_ORDER_PROPERTY),
            DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)),
        tags = filterStatusTags(task.tags, columns),
        before = checkAndCreateRepeatingTask(task)
            .mapLeft {
                if (it.isError) console.log("Cannot create repeating task", it)
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

    console.log("ReducerUtils.changedTasks()", fileTasks, storeFileTasks)
    return fileTasks.minus(storeFileTasks.toSet())
}
