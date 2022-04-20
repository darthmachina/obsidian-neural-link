package neurallink.core.service

import arrow.core.Either
import arrow.core.flatMap
import neurallink.core.model.Task
import neurallink.core.store.deepCopy
import arrow.optics.snoc
import neurallink.core.model.Tag
import neurallink.core.model.TaskConstants
import neurallink.core.model.TaskId

/**
 * Creates a copy of the task if Task.original has not already been set,
 * if it has just return Task.original
 */
fun getOriginal(task: Task): Task {
    return task.original ?: task.deepCopy()
}

fun filterTags(tags: Set<Tag>, filter: (Tag) -> Boolean): Set<Tag> {
    return tags.filter(filter).toSet()
}

fun addTag(tags: Set<Tag>, tag: Tag): Set<Tag> {
    return (tags.toList() snoc tag).toSet()
}

// Find functions
fun List<Task>.findById(id: TaskId) : Either<String,Task> {
    val task = this.find { it.id == id }
    return if (task == null) {
        Either.Left("Task not found with ID $id")
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
