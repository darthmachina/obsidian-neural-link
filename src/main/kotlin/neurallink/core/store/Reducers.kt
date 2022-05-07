package neurallink.core.store

import arrow.core.getOrElse
import neurallink.core.model.*
import neurallink.core.service.completeTask
import neurallink.core.service.filterTasks
import neurallink.core.service.kanban.createKanbanMap
import neurallink.core.service.kanban.findEndPosition
import neurallink.core.service.kanban.findPosition
import neurallink.core.service.kanban.getStatusTagFromTask
import neurallink.core.service.toJson
import org.reduxkotlin.Reducer

val reducerFunctions = Reducers()

val reducer: Reducer<NeuralLinkModel> = { store, action ->
    when (action) {
        is VaultLoaded -> reducerFunctions.copyAndPopulateKanban(store, action.tasks)
        is TaskMoved -> reducerFunctions.moveCard(store, action.taskId, action.newStatus, action.beforeTask)
        is MoveToTop -> reducerFunctions.moveToTop(store, action.taskd)
        is ModifyFileTasks -> reducerFunctions.modifyFileTasks(store, action.file, action.fileTasks)
        is TaskCompleted -> reducerFunctions.taskCompleted(store, action.taskId, action.subtaskChoice)
        is SubtaskCompleted -> reducerFunctions.markSubtaskCompletion(store, action.taskId, action.subtaskId, action.complete)
        is RepeatTask -> store
        is FilterByTag -> reducerFunctions.filterByTag(store, action.tag)
        is FilterByFile -> reducerFunctions.filterByFile(store, action.file)
        is FilterByDataviewValue -> reducerFunctions.filterByDataviewValue(store, action.value)
        is FilterFutureDate -> reducerFunctions.filterFutureDate(store, action.filter)
        is UpdateSettings -> reducerFunctions.updateSettings(store, action)
        else -> store
    }
}

class Reducers {
    /**
     * Updates the settings, if any update value is null it will reuse the value in the store to allow for partial
     * updates.
     */
    fun updateSettings(store: NeuralLinkModel, updateSettings: UpdateSettings): NeuralLinkModel {
        console.log("updateSettings()")
        val newSettings = store.settings.copy(
            taskRemoveRegex = updateSettings.taskRemoveRegex ?: store.settings.taskRemoveRegex,
            columnTags = updateSettings.columnTags ?: store.settings.columnTags,
            tagColors = updateSettings.tagColors ?: store.settings.tagColors
        )
        updateSettings.plugin.saveData(toJson(newSettings))
        return if (updateSettings.columnTags != null) {
//            console.log(" - columns updated, reloading kanban")
            val clonedTaskList = store.tasks.map { it }
            store.copy(
                settings = newSettings,
                tasks = clonedTaskList,
                kanbanColumns = createKanbanMap(
                    filterTasks(clonedTaskList, store.filterValue),
                    newSettings.columnTags
                )
            )
        } else {
            store.copy(
                settings = newSettings
            )
        }
    }

    /**
     * Called when the vault is initially loaded with a task list, will populate the kanban data
     */
    fun copyAndPopulateKanban(store: NeuralLinkModel, tasks: List<Task>): NeuralLinkModel {
        console.log("Reducers.copyAndPopulateKanban()")
        return store.copy(
            tasks = tasks,
            kanbanColumns = createKanbanMap(
                filterTasks(tasks, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun moveCard(store: NeuralLinkModel, taskId: TaskId, newStatus: StatusTag, beforeTaskId: TaskId?): NeuralLinkModel {
        console.log("Reducers.taskStatusChanged()")
        if (store.tasks.find { it.id == taskId } == null) {
            console.log(" - ERROR: Task not found for id: $taskId")
            return store
        }

        val clonedTaskList = store.tasks.map { task ->
            if (task.id == taskId) {
                val oldStatus = getStatusTagFromTask(task, store.settings.columnTags)
                task.copy(
                    original = task.original ?: task.deepCopy(),
                    tags = task.tags
                        .filter { tag ->
                            oldStatus.map { it.tag != tag }.getOrElse { true }
                        }
                        .plus(newStatus.tag)
                        .toSet(),
                    dataviewFields = task.dataviewFields.entries.associate { entry ->
                        if (entry.key == DataviewField(TaskConstants.TASK_ORDER_PROPERTY)) {
                            findPosition(store.tasks, newStatus, beforeTaskId)
                                .map {
                                    DataviewField(TaskConstants.TASK_ORDER_PROPERTY) to DataviewValue(it)
                                }
                                .mapLeft {
                                    console.log("ERROR finding position in new list", it)
                                }
                                .getOrElse {
                                    // No position found
                                    DataviewField(TaskConstants.TASK_ORDER_PROPERTY) to DataviewValue(
                                        findEndPosition(store.tasks, newStatus)
                                    )
                                }


                        } else {
                            entry.key to entry.value
                        }
                    }.toDataviewMap()
                )
            } else {
                task
            }
        }
        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = createKanbanMap(
                filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun moveToTop(store: NeuralLinkModel, taskId: TaskId) : NeuralLinkModel {
        val clonedTaskList = store.tasks.map { task ->
            if (task.id == taskId) {
                task.copy(
                    original = task.original ?: task.deepCopy(),
                    dataviewFields = task.dataviewFields.entries.associate { entry ->
                        if (entry.key == DataviewField(TaskConstants.TASK_ORDER_PROPERTY)) {
                            DataviewField(TaskConstants.TASK_ORDER_PROPERTY) to DataviewValue(
                                store.tasks
                                    .filter { task ->
                                        getStatusTagFromTask(task, store.settings.columnTags)
                                            .map {
                                                task.tags.contains(it.tag)
                                            }
                                            .getOrElse { false }
                                    }
                                    .sortedWith(compareBy(nullsLast()) { task -> task.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).orNull()?.asDouble() })
                                    .first()
                                    .dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).getOrElse { DataviewValue(0.0) }.asDouble() / 2)
                        } else {
                            entry.key to entry.value
                        }
                    }.toDataviewMap()
                )
            } else {
                task
            }
        }
        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = createKanbanMap(
                filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun taskCompleted(
        store: NeuralLinkModel,
        taskId: TaskId,
        subtaskChoice: IncompleteSubtaskChoice
    ): NeuralLinkModel {
        console.log("Reducers.taskCompleted()")
        val clonedTaskList = store.tasks.map { task ->
            if (task.id == taskId) {
                completeTask(task, subtaskChoice, store.settings.columnTags)
            } else {
                task
            }
        }

        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = createKanbanMap(
                filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun markSubtaskCompletion(store: NeuralLinkModel, taskId: TaskId, subtaskId: TaskId, complete: Boolean): NeuralLinkModel {
        console.log("Reducers.subtaskCompleted()")
        if (store.tasks.find { it.id == taskId } == null) {
            console.log(" - ERROR: Task not found for id: $taskId")
            return store
        }

        val clonedTaskList = store.tasks.map { task ->
            if (task.id == taskId) {
                task.copy(
                    original = task.original ?: task.deepCopy(),
                    subtasks = task.subtasks.map { subtask ->
                        if (subtask.id == subtaskId) {
                            subtask.copy(completed = complete)
                        } else {
                            subtask
                        }
                    }
                )
            } else {
                task
            }
        }
        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = createKanbanMap(
                filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun modifyFileTasks(store: NeuralLinkModel, file: TaskFile, fileTasks: List<Task>): NeuralLinkModel {
        console.log("Reducers.modifyFileTasks()")
        // Only return a new state if any of the tasks were modified
        val clonedTaskList = store.tasks
            .filter { it.file != file }
            .plus(ReducerUtils.runFileModifiedListeners(fileTasks, store))

        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = createKanbanMap(
                filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    /**
     * Filters the task list according to the given tag; a null tag means there should be no filter.
     */
    fun filterByTag(store: NeuralLinkModel, tag: String?) : NeuralLinkModel {
        val filterValue = TagFilterValue(Tag(tag ?: ""))
        return store.copy(
            kanbanColumns = createKanbanMap(
                filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        )
    }

    fun filterByFile(store: NeuralLinkModel, file: String?) : NeuralLinkModel {
        val filterValue = FileFilterValue(TaskFile(file ?: ""))
        return store.copy(
            kanbanColumns = createKanbanMap(
                filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        )
    }

    fun filterByDataviewValue(store: NeuralLinkModel, value: String?) : NeuralLinkModel {
        val dataview = value?.split("::") ?: throw IllegalStateException("Filter value is not a valid dataview field '$value'")
        val filterValue = DataviewFilterValue(DataviewPair(DataviewField(dataview[0]) to DataviewValue(dataview[1])))
        return store.copy(
            kanbanColumns = createKanbanMap(
                filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        )
    }

    fun filterFutureDate(store: NeuralLinkModel, filter: Boolean) : NeuralLinkModel {
        val filterValue = FutureDateFilterValue(filter)
        return store.copy(
            kanbanColumns = createKanbanMap(
                filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        )
    }
}

class ReducerUtils {
    companion object {

        fun runFileModifiedListeners(tasks: List<Task>, store: NeuralLinkModel) : List<Task> {
            console.log("Reducers.ReducerUtils.runFileModifiedListeners()", tasks)

            // Check for completed tasks with either a status tag or a repeat field (might have been completed outside the app)
            // remove the status tag and check for any tasks that need repeating. Do nothing with subtasks as we are outside
            // the app.
            var newTasks = tasks
                .map { task ->
                    if (task.completed &&
                                (getStatusTagFromTask(task, store.settings.columnTags).isRight()
                                || task.dataviewFields.keys.contains(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)))) {
                        completeTask(task, IncompleteSubtaskChoice.NOTHING, store.settings.columnTags)
                    } else {
                        task
                    }
                }
            console.log("newTasks after checking for completed", newTasks)

            // Check for tasks with no position
            newTasks = newTasks
                .map { task ->
                    val statusTag = getStatusTagFromTask(task, store.settings.columnTags)
                    if (!task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)) &&
                            !statusTag.map { it.dateSort }.getOrElse { false }
                    ) {
                        task.copy(
                            original = task.original ?: task.deepCopy(),
                            dataviewFields = getStatusTagFromTask(task, store.settings.columnTags)
                                .map { findEndPosition(store.tasks, it) }
                                .map {
                                    task.dataviewFields
                                        .plus(DataviewField(TaskConstants.TASK_ORDER_PROPERTY) to DataviewValue(it))
                                        .toDataviewMap()
                                }.getOrElse {
                                    task.dataviewFields
                                }
                        )
                    } else {
                        task
                    }
                }
            return newTasks
        }
    }
}
