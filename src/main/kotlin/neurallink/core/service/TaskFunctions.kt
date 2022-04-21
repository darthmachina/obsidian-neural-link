package neurallink.core.service

import arrow.core.Either
import arrow.core.flatMap
import neurallink.core.store.deepCopy
import arrow.optics.snoc
import neurallink.core.model.*
import neurallink.core.store.IncompleteSubtaskChoice

/**
 * Creates a copy of the task if Task.original has not already been set,
 * if it has just return Task.original
 */
fun getOriginal(task: Task): Task {
    return task.original ?: task.deepCopy()
}

// Tags
fun filterTags(tags: Set<Tag>, filter: (Tag) -> Boolean): Set<Tag> {
    return tags.filter(filter).toSet()
}

fun addTag(tags: Set<Tag>, tag: Tag): Set<Tag> {
    return (tags.toList() snoc tag).toSet()
}

// Dataview
fun Map<DataviewField,DataviewValue>.toDataviewMap() : DataviewMap {
    return DataviewMap(this)
}

fun DataviewMap.removeKeys(vararg fields: DataviewField) : DataviewMap {
    return this
        .filter {
            it.key in fields
        }
        .toDataviewMap()
}

// Subtasks
fun completedTaskSubtasks(subtasks: List<Task>, choice: IncompleteSubtaskChoice) : List<Task> {
    return when (choice) {
        IncompleteSubtaskChoice.DELETE -> subtasks.filter { it.completed }
        IncompleteSubtaskChoice.COMPLETE -> {
            subtasks.map {
                if (it.completed) {
                    it
                } else {
                    it.copy(completed = true)
                }
            }
        }
        else -> subtasks
    }
}

// Find functions
fun List<Task>.findById(id: TaskId) : Either<Problem,Task> {
    val task = this.find { it.id == id }
    return if (task == null) {
        Either.Left(TaskNotFoundProblem(id))
    } else {
        Either.Right(task)
    }
}

// Filtering functions
fun List<Task>.filterByTag(tag: Tag) : List<Task> {
    return this.filter { task -> task.tags.contains(tag) }
}

// Sorting functions
fun List<Task>.sortByTaskOrder() : List<Task> {
    return this.sortedWith(compareBy(nullsLast()) { task ->
        task.dataviewFields.valueForField(TaskConstants.TASK_ORDER_PROPERTY)
            .flatMap { it.asDouble() }
            .orNull()
    })
}
