package neurallink.core.store

import Notice
import arrow.core.*
import mu.KotlinLogging
import mu.KotlinLoggingConfiguration
import neurallink.core.model.*
import neurallink.core.service.*
import neurallink.core.service.kanban.createKanbanMap
import neurallink.core.service.kanban.findEndPosition
import neurallink.core.service.kanban.findPosition
import neurallink.core.service.kanban.getStatusTagFromTask
import neurallink.view.ViewConstants
import org.reduxkotlin.Reducer

private val logger = KotlinLogging.logger("Reducers")

val reducerFunctions = Reducers()

val reducer: Reducer<NeuralLinkModel> = { store, action ->
    when (action) {
        is VaultLoaded -> handleError(action, reducerFunctions.copyAndPopulateKanban(store, action.tasks), store)
        is FileDeleted -> handleError(action, reducerFunctions.removeTasksForFile(store, action.file), store)
        is FileCreated -> handleError(action, reducerFunctions.fileCreated(store, action.fileTasks), store)
        is TaskMoved -> handleError(action, reducerFunctions.moveCard(store, action.taskId, action.newStatus, action.beforeTask), store)
        is MoveToTop -> handleError(action, reducerFunctions.moveToTop(store, action.taskd), store)
        is ModifyFileTasks -> handleError(action, reducerFunctions.modifyFileTasks(store, action.file, action.fileTasks), store)
        is TaskCompleted -> handleError(action, reducerFunctions.taskCompleted(store, action.taskId, action.subtaskChoice), store)
        is SubtaskCompleted -> handleError(action, reducerFunctions.markSubtaskCompletion(store, action.taskId, action.subtaskId, action.complete), store)
        is RepeatTask -> store
        is FilterByTag -> handleError(action, reducerFunctions.filterByTag(store, action.tag), store)
        is FilterByFile -> handleError(action, reducerFunctions.filterByFile(store, action.file), store)
        is FilterByDataviewValue -> handleError(action, reducerFunctions.filterByDataviewValue(store, action.value), store)
        is FilterFutureDate -> handleError(action, reducerFunctions.filterFutureDate(store, action.filter), store)
        is UpdateSettings -> handleError(action, reducerFunctions.updateSettings(store, action), store)
        else -> store
    }
}

fun handleError(action: Action, maybeError: Either<NeuralLinkError, NeuralLinkModel>, existingModel: NeuralLinkModel) : NeuralLinkModel {
    return maybeError.getOrHandle {
        // Is an Either.Left, show a notice and return the existing model
        logger.error(it.throwable) { "${action::class.simpleName}: ${it.message}" }
        Notice("${action::class.simpleName}: ERROR: ${it.message}", ViewConstants.NOTICE_TIMEOUT)
        existingModel
    }
}

/**
 * All reducer functions. Should return Either<NeuralLinkError,NeuralLinkModel> to be handled by `handleError`
 * to convert the Either into whichever model is appropriate (new or existing).
 *
 * TODO: Create wrapper for createKanbanMap() to remove need for boilerplate code.
 */
class Reducers {
    /**
     * Updates the settings, if any update value is null it will reuse the value in the store to allow for partial
     * updates.
     */
    fun updateSettings(store: NeuralLinkModel, updateSettings: UpdateSettings): Either<NeuralLinkError,NeuralLinkModel> {
        logger.debug { "updateSettings() : $updateSettings" }
        val newSettings = store.settings.copy(
            taskRemoveRegex = updateSettings.taskRemoveRegex ?: store.settings.taskRemoveRegex,
            columnTags = updateSettings.columnTags ?: store.settings.columnTags,
            tagColors = updateSettings.tagColors ?: store.settings.tagColors,
            logLevel = updateSettings.logLevel ?: store.settings.logLevel,
            ignorePaths = updateSettings.ignorePaths ?: store.settings.ignorePaths
        )
        updateSettings.plugin.saveData(toJson(newSettings))
        return if (updateSettings.columnTags != null) {
            val clonedTaskList = store.tasks.map { it }
            store.copy(
                settings = newSettings,
                tasks = clonedTaskList,
                kanbanColumns = createKanbanMap(
                    filterTasks(clonedTaskList, store.filterValue),
                    newSettings.columnTags
                )
            ).right()
        } else if (updateSettings.ignorePaths != null) {
            val clonedTaskList = store.tasks.filter { task -> !pathInPathList(task.file.value, updateSettings.ignorePaths) }
            store.copy(
                settings = newSettings,
                tasks = clonedTaskList,
                kanbanColumns = createKanbanMap(
                    filterTasks(clonedTaskList, store.filterValue),
                    newSettings.columnTags
                )
            ).right()
        } else {
            if (updateSettings.logLevel != null) {
                KotlinLoggingConfiguration.LOG_LEVEL = updateSettings.logLevel
            }
            store.copy(
                settings = newSettings
            ).right()
        }
    }

    /**
     * Called when the vault is initially loaded with a task list, will populate the kanban data
     */
    fun copyAndPopulateKanban(store: NeuralLinkModel, tasks: List<Task>): Either<NeuralLinkError,NeuralLinkModel> {
        logger.debug { "copyAndPopulateKanban()" }
        return store.copy(
            tasks = tasks,
            sourceFiles = getAllSourceFiles(tasks),
            kanbanColumns = createKanbanMap(
                filterTasks(tasks, store.filterValue),
                store.settings.columnTags
            )
        ).right()
    }

    fun fileCreated(store: NeuralLinkModel, tasks: List<Task>): Either<NeuralLinkError, NeuralLinkModel> {
        logger.debug { "fileCreated()" }
        val allTasks = store.tasks.plus(tasks)
        return store.copy(
            tasks = allTasks,
            sourceFiles = getAllSourceFiles(allTasks),
            kanbanColumns = createKanbanMap(
                filterTasks(allTasks, store.filterValue),
                store.settings.columnTags
            )
        ).right()
    }

    fun removeTasksForFile(store: NeuralLinkModel, file: TaskFile): Either<NeuralLinkError, NeuralLinkModel> {
        logger.debug { "removeTasksForFile() : $file" }
        return removeTasksForFile(store.tasks, file)
            .map { tasks ->
                store.copy(
                    tasks = tasks,
                    sourceFiles = getAllSourceFiles(tasks), // TODO Try to just remove file that was deleted
                    kanbanColumns = createKanbanMap(
                        filterTasks(tasks, store.filterValue),
                        store.settings.columnTags
                    )
                )
            }
    }

    fun moveCard(store: NeuralLinkModel, taskId: TaskId, newStatus: StatusTag, beforeTaskId: TaskId?): Either<NeuralLinkError,NeuralLinkModel> {
        logger.debug { "taskStatusChanged()" }
        if (store.tasks.find { it.id == taskId } == null) {
            TaskNotFoundError(taskId).left()
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
                                    logger.error { "ERROR finding position in new list: $it" }
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
        ).right()
    }

    fun moveToTop(store: NeuralLinkModel, taskId: TaskId) : Either<NeuralLinkError,NeuralLinkModel> {
        logger.debug { "moveToTop()" }
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
        ).right()
    }

    fun taskCompleted(
        store: NeuralLinkModel,
        taskId: TaskId,
        subtaskChoice: IncompleteSubtaskChoice
    ): Either<NeuralLinkError,NeuralLinkModel> {
        logger.debug { "taskCompleted()" }
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
        ).right()
    }

    fun markSubtaskCompletion(store: NeuralLinkModel, taskId: TaskId, subtaskId: TaskId, complete: Boolean): Either<NeuralLinkError,NeuralLinkModel> {
        logger.debug { "subtaskCompleted()" }
        if (store.tasks.find { it.id == taskId } == null) {
            TaskNotFoundError(taskId).left()
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
        ).right()
    }

    fun modifyFileTasks(store: NeuralLinkModel, file: TaskFile, fileTasks: List<Task>): Either<NeuralLinkError,NeuralLinkModel> {
        logger.debug { "modifyFileTasks(): ${file.value}" }
        // Only return a new state if any of the tasks were modified
        val clonedTaskList = store.tasks
            .filter { it.file != file }
            .plus(ReducerUtils.runFileModifiedListeners(fileTasks, store))

        return store.copy(
            tasks = clonedTaskList,
            sourceFiles = if (store.sourceFiles.contains(file.value.dropLast(3))) store.sourceFiles else getAllSourceFiles(store.tasks),
            kanbanColumns = createKanbanMap(
                filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        ).right()
    }

    /**
     * Filters the task list according to the given tag; a null tag means there should be no filter.
     */
    fun filterByTag(store: NeuralLinkModel, tag: String?) : Either<NeuralLinkError,NeuralLinkModel> {
        val filterValue = if (tag != null) TagFilterValue(Tag(tag)) else NoneFilterValue()
        return store.copy(
            kanbanColumns = createKanbanMap(
                filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        ).right()
    }

    fun filterByFile(store: NeuralLinkModel, file: String?) : Either<NeuralLinkError,NeuralLinkModel> {
        logger.debug { "filterByFile(): $file" }
        val filterValue = if (file != null) FileFilterValue(TaskFile(file)) else NoneFilterValue()
        return store.copy(
            kanbanColumns = createKanbanMap(
                filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        ).right()
    }

    fun filterByDataviewValue(store: NeuralLinkModel, value: String?) : Either<NeuralLinkError,NeuralLinkModel> {
        val filterValue = if (value != null) {
            val dataview =
                value?.split("::")
            DataviewFilterValue(DataviewPair(DataviewField(dataview[0]) to DataviewValue(dataview[1])))
        } else {
            NoneFilterValue()
        }
        return store.copy(
            kanbanColumns = createKanbanMap(
                filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        ).right()
    }

    fun filterFutureDate(store: NeuralLinkModel, filter: Boolean) : Either<NeuralLinkError,NeuralLinkModel> {
        val filterValue = FutureDateFilterValue(filter)
        return store.copy(
            kanbanColumns = createKanbanMap(
                filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        ).right()
    }
}

class ReducerUtils {
    companion object {
        fun runFileModifiedListeners(tasks: List<Task>, store: NeuralLinkModel) : List<Task> {
            logger.debug { "runFileModifiedListeners()" }
            logger.trace { " - $tasks" }

            // Check for completed tasks with either a status tag or a repeat field (might have been completed outside the app)
            // remove the status tag and check for any tasks that need repeating. Do nothing with subtasks as we are outside
            // the app.
            var newTasks = tasks
                .mapNotNull { task ->
                    if (task.completed &&
                                (getStatusTagFromTask(task, store.settings.columnTags).isRight()
                                || task.dataviewFields.keys.contains(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)))) {
                        completeTask(task, IncompleteSubtaskChoice.NOTHING, store.settings.columnTags)
                    } else {
                        task
                    }
                }
            logger.trace { "newTasks after checking for completed : $newTasks" }

            // Check for tasks with no position
            newTasks = newTasks
                .map { task ->
                    getStatusTagFromTask(task, store.settings.columnTags)
                        .map { statusTag ->
                            if (statusTag.dateSort) {
                                // Column sorted by date, no 'pos' field used
                                task
                            } else if (task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_ORDER_PROPERTY))) {
                                // Task already has a 'pos' field
                                task
                            } else {
                                // Not date sorted and the 'pos' field does not exist, update task
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
                            }
                        }
                        .getOrElse { task }
                }
            return newTasks
        }
    }
}
