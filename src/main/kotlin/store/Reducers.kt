package store

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import model.*
import neurallink.core.model.*
import org.reduxkotlin.Reducer
import service.RepeatingTaskService

val reducerFunctions = Reducers()

val reducer: Reducer<TaskModel> = { store, action ->
    when (action) {
        is VaultLoaded -> reducerFunctions.copyAndPopulateKanban(store, action.tasks)
        is TaskMoved -> reducerFunctions.moveCard(store, action.taskId, action.newStatus, action.beforeTask)
        is MoveToTop -> reducerFunctions.moveToTop(store, action.taskd)
        is ModifyFileTasks -> reducerFunctions.modifyFileTasks(store, action.file, action.fileTasks, action.repeatingTaskService)
        is TaskCompleted -> reducerFunctions.taskCompleted(store, action.taskId, action.subtaskChoice, action.repeatingTaskService)
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
    fun updateSettings(store: TaskModel, updateSettings: UpdateSettings): TaskModel {
        console.log("updateSettings()")
        val newSettings = store.settings.copy(
            taskRemoveRegex = updateSettings.taskRemoveRegex ?: store.settings.taskRemoveRegex,
            columnTags = updateSettings.columnTags ?: store.settings.columnTags,
            tagColors = updateSettings.tagColors ?: store.settings.tagColors
        )
        updateSettings.plugin.saveData(updateSettings.settingsService.toJson(newSettings))
        return if (updateSettings.columnTags != null) {
//            console.log(" - columns updated, reloading kanban")
            val clonedTaskList = store.tasks.map { it }
            store.copy(
                settings = newSettings,
                tasks = clonedTaskList,
                kanbanColumns = ReducerUtils.createKanbanMap(
                    ReducerUtils.filterTasks(clonedTaskList, store.filterValue),
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
    fun copyAndPopulateKanban(store: TaskModel, tasks: List<Task>): TaskModel {
        console.log("Reducers.copyAndPopulateKanban()")
        return store.copy(
            tasks = tasks,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(tasks, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun moveCard(store: TaskModel, taskId: TaskId, newStatus: StatusTag, beforeTaskId: TaskId?): TaskModel {
        console.log("Reducers.taskStatusChanged()")
        if (store.tasks.find { it.id == taskId } == null) {
            console.log(" - ERROR: Task not found for id: $taskId")
            return store
        }

        val clonedTaskList = store.tasks.map { task ->
            if (task.id == taskId) {
                val oldStatus = ReducerUtils.getStatusTagFromTask(task, store.settings.columnTags)!!
                task.copy(
                    original = task.original ?: task.deepCopy(),
                    tags = task.tags.filter { it != oldStatus.tag }.toSet(),
                    dataviewFields = task.dataviewFields.entries.associate { entry ->
                        if (entry.key == DataviewField(TaskConstants.TASK_ORDER_PROPERTY)) {
                            DataviewField(TaskConstants.TASK_ORDER_PROPERTY) to DataviewValue(ReducerUtils.findPosition(store.tasks, newStatus, beforeTaskId))
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
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun moveToTop(store: TaskModel, taskId: TaskId) : TaskModel {
        val clonedTaskList = store.tasks.map { task ->
            if (task.id == taskId) {
                task.copy(
                    original = task.original ?: task.deepCopy(),
                    dataviewFields = task.dataviewFields.entries.associate { entry ->
                        if (entry.key == DataviewField(TaskConstants.TASK_ORDER_PROPERTY)) {
                            DataviewField(TaskConstants.TASK_ORDER_PROPERTY) to DataviewValue(
                                store.tasks
                                    .filter { task -> task.tags.contains(ReducerUtils.getStatusTagFromTask(task, store.settings.columnTags)?.tag) }
                                    .sortedWith(compareBy(nullsLast()) { task -> task.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).asDouble() })
                                    .first()
                                    .dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).asDouble() / 2)
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
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun taskCompleted(
        store: TaskModel,
        taskId: TaskId,
        subtaskChoice: IncompleteSubtaskChoice,
        repeatingTaskService: RepeatingTaskService
    ): TaskModel {
        console.log("Reducers.taskCompleted()")
        val clonedTaskList = store.tasks.map { it.deepCopy() }
        console.log(" - store and cloned list", store, clonedTaskList)
        val task = clonedTaskList.find { task -> task.id == taskId }
        if (task == null) {
            console.log(" - ERROR: Task not found for id: $taskId")
            return store
        }
        ReducerUtils.completeTask(task, subtaskChoice, store.settings.columnTags, repeatingTaskService)
        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun markSubtaskCompletion(store: TaskModel, taskId: TaskId, subtaskId: TaskId, complete: Boolean): TaskModel {
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
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun modifyFileTasks(store: TaskModel, file: TaskFile, fileTasks: List<Task>, repeatingTaskService: RepeatingTaskService): TaskModel {
        console.log("Reducers.modifyFileTasks()")
        val modifiedFiles = ReducerUtils.runFileModifiedListeners(fileTasks, store, repeatingTaskService)
        // Only return a new state if any of the tasks were modified
        val clonedTaskList = store.tasks
            .filter { it.file != file }
            .plus(ReducerUtils.runFileModifiedListeners(fileTasks, store, repeatingTaskService))

        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    /**
     * Filters the task list according to the given tag; a null tag means there should be no filter.
     */
    fun filterByTag(store: TaskModel, tag: String?) : TaskModel {
        val filterValue = TagFilterValue(Tag(tag ?: ""))
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        )
    }

    fun filterByFile(store: TaskModel, file: String?) : TaskModel {
        val filterValue = FileFilterValue(TaskFile(file ?: ""))
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        )
    }

    fun filterByDataviewValue(store: TaskModel, value: String?) : TaskModel {
        val dataview = value?.split("::") ?: throw IllegalStateException("Filter value is not a valid dataview field '$value'")
        val filterValue = DataviewFilterValue(DataviewPair<String>(DataviewField(dataview[0]) to DataviewValue(dataview[1])))
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        )
    }

    fun filterFutureDate(store: TaskModel, filter: Boolean) : TaskModel {
        val filterValue = FutureDateFilterValue(filter)
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, filterValue),
                store.settings.columnTags
            ),
            filterValue = filterValue
        )
    }
}

class ReducerUtils {
    companion object {
        private val taskComparator = compareBy<Task,Double?>(nullsLast()) {
            val position = it.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).asDouble()
            position
        }

        private val taskDateComparator = compareBy<Task,LocalDate?>(nullsFirst()) {
            it.dueOn?.value
        }

        fun filterTasks(tasks: List<Task>, filterValue: FilterValue<out Any>) : List<Task> {
            return when (filterValue) {
                is NoneFilterValue -> tasks
                is TagFilterValue -> tasks.filter { task -> task.tags.contains(filterValue.filterValue) }
                is FileFilterValue -> tasks.filter { task -> task.file == filterValue.filterValue }
                is DataviewFilterValue -> tasks.filter { task ->
                    task.dataviewFields.containsKey(filterValue.filterValue.value.first)
                    // TODO This is only filtering on the key being present, not using the value at all
                }
                is FutureDateFilterValue -> {
                    val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) // TODO Is TimeZone here going to affect anything?
                    val currentDate = LocalDate(currentDateTime.year, currentDateTime.month, currentDateTime.dayOfMonth)
                    console.log("Filtering using $currentDate")
                    tasks.filter {
                        if (filterValue.filterValue) {
                            if (it.dueOn == null) true else it.dueOn <= currentDate
                        } else {
                            true
                        }
                    }
                }
            }
        }

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
                .groupBy { task -> getStatusTagFromTask(task, statusTags)!! }
                .plus(statusTags.minus(getAllStatusTagsOnTasks(tasks, statusTags))
                .map { statusTag -> Pair(statusTag, emptyList())})
                .mapValues { it.value.sortedWith(if (it.key.dateSort) taskDateComparator else taskComparator) }
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
//            console.log("Reducers.ReducerUtils.addOrderToListItems()")
            var maxPosition = 1.0
            return tasks
                .sortedWith(taskComparator)
                .map { task ->
                    if (task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_ORDER_PROPERTY))) {
                        updateTaskOrder(task, maxPosition++)
                    } else {
                        maxPosition = task.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).asDouble()
                        task
                    }
            }
        }

        private fun getAllStatusTagsOnTasks(tasks: List<Task>, statusTags: List<StatusTag>) : Set<StatusTag> {
//            console.log("Reducers.ReducerUtils.getAllStatusTagsOnTasks()", tasks, statusTags)
            return tasks
                .asSequence()
                .map { task -> task.tags }
                .flatten()
                .distinct()
                .filter { tag -> tag in statusTags.map{ it.tag } }
                .map { tag -> statusTags.find { statusTag -> statusTag.tag == tag }!! }
                .toSet()
        }

        /**
         * Finds the position for a task, calculated given the following scenarios:
         *
         * 1. No tasks for the status, returns 1.0 (to leave room before it for other cards)
         * 2. No beforeTaskId given, returns the max position value + 1 to put it at the end
         * 3. beforeTaskId given, find that task and the one before it and returns a value in the middle of the two pos values
         *  - If beforeTask is the first in the list just return its position divided by 2
         */
        fun findPosition(tasks: List<Task>, status: StatusTag, beforeTaskId: TaskId? = null) : Double {
            console.log("ReducerUtils.findPosition()")
            return if (tasks.none { task -> task.tags.contains(status.tag) }) {
//                console.log(" - list is empty, returning 1.0")
                1.0
            } else if (beforeTaskId == null) {
//                console.log(" - no beforeTaskId, adding to end of list")
                (tasks
                    .filter { task -> task.tags.contains(status.tag) }
                    .sortedWith(compareBy(nullsLast()) { task -> task.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).asDouble() })
                    .last()
                    .dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).asDouble()) + 1.0
            } else {
//                console.log(" - beforeTaskId set, finding new position")
                val statusTasks = tasks
                    .filter { task -> task.tags.contains(status.tag) }
                    .sortedWith(compareBy(nullsLast()) { task -> task.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).asDouble() })
                val beforeTask = statusTasks.find { it.id == beforeTaskId }
                    ?: throw IllegalStateException("beforeTask not found for id $beforeTaskId")
                val beforeTaskPosition = beforeTask.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).asDouble()
                val beforeTaskIndex = statusTasks.indexOf(beforeTask)
                // Returns new position
                if (beforeTaskIndex == 0) {
                    beforeTaskPosition / 2
                } else {
                    val beforeBeforeTask = statusTasks[beforeTaskIndex - 1]
                    val beforeBeforeTaskPosition = beforeBeforeTask.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).asDouble()
                    (beforeTaskPosition + beforeBeforeTaskPosition) / 2
                }
            }
        }

        /**
         * Returns a list of Tasks from a file that have changed from within the store
         */
        fun changedTasks(file: String, fileTasks: List<Task>, store: TaskModel) : List<Task> {
            // Take the fileTasks list and subtrack any that are equal to what is already in the store
            val storeFileTasks = store.tasks.filter { it.file.value == file }
            if (storeFileTasks.isEmpty()) return emptyList()

            console.log("ReducerUtils.changedTasks()", fileTasks, storeFileTasks)
            return fileTasks.minus(storeFileTasks.toSet())
        }

        fun runFileModifiedListeners(tasks: List<Task>, store: TaskModel, repeatingTaskService: RepeatingTaskService) : List<Task> {
            console.log("Reducers.ReducerUtils.runFileModifiedListeners()", tasks)

            // Check for completed tasks with either a status tag or a repeat field (might have been completed outside the app)
            // remove the status tag and check for any tasks that need repeating. Do nothing with subtasks as we are outside
            // the app.
            var newTasks = tasks
                .map { task ->
                    if (task.completed &&
                                (getStatusTagFromTask(task, store.settings.columnTags) != null
                                || task.dataviewFields.keys.contains(DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)))) {
                        completeTask(task, IncompleteSubtaskChoice.NOTHING, store.settings.columnTags, repeatingTaskService)
                    } else {
                        task
                    }
                }

            // Check for tasks with no position
            newTasks = newTasks
                .map { task ->
                    val statusTag = getStatusTagFromTask(task, store.settings.columnTags)
                    if (!task.dataviewFields.containsKey(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)) &&
                            statusTag != null &&
                            !statusTag.dateSort
                    ) {
                        task.copy(
                            original = task.original ?: task.deepCopy(),
                            dataviewFields = task.dataviewFields
                                .plus(DataviewField(TaskConstants.TASK_ORDER_PROPERTY) to DataviewValue(
                                    findPosition(store.tasks, getStatusTagFromTask(task, store.settings.columnTags)!!))
                                ).toDataviewMap()
                        )
                    } else {
                        task
                    }
                }
            return newTasks
        }

        fun completeTask(
            task: Task,
            subtaskChoice: IncompleteSubtaskChoice,
            columns: Collection<StatusTag>,
            repeatingTaskService: RepeatingTaskService
        ) : Task {
            return task.copy(
                original = task.original ?: task.deepCopy(),
                completed = true,
                subtasks = task.subtasks
                    .filter { subtask ->
                        if (subtaskChoice == IncompleteSubtaskChoice.DELETE) {
                            !subtask.completed
                        } else {
                            true
                        }
                    }
                    .map { subtask ->
                        if (subtaskChoice == IncompleteSubtaskChoice.COMPLETE) {
                            subtask.copy(completed = true)
                        } else {
                            subtask
                        }
                    },
                dataviewFields = task.dataviewFields
                    .filterKeys { key ->
                        key != DataviewField(TaskConstants.TASK_ORDER_PROPERTY) && // Remove Order
                                (repeatingTaskService.isTaskRepeating(task) && key != DataviewField(TaskConstants.TASK_REPEAT_PROPERTY)) // If repeating remove repeat
                    }.toDataviewMap(),
                tags = task.tags.filter { tag -> tag in columns.map { it.tag } }.toSet(),
                before = if (repeatingTaskService.isTaskRepeating(task)) repeatingTaskService.getNextRepeatingTask(task) else null
            )
        }

        fun getStatusTagFromTask(task: Task, kanbanKeys: Collection<StatusTag>): StatusTag? {
//            console.log("Reducers.ReducerUtils.getStatusTagFromTask()", task)
            val statusColumn = kanbanKeys.filter { statusTag -> task.tags.contains(statusTag.tag) }
            if (statusColumn.size > 1) {
                console.log(" - WARN: More than one status column is on the task, using the first: ", statusColumn)
                return statusColumn[0]
            } else if (statusColumn.size == 1) {
                return statusColumn[0]
            }
            // No status tag found
            return null
        }

        /**
         * Updates the task order for a task if required (order is null or already set to the given value), saving the
         * original before making the change.
         */
        fun updateTaskOrder(task: Task, position: Double): Task {
            console.log("ReducerUtils.updateTaskOrder()", task, position)
            val taskOrder = task.dataviewFields[DataviewField(TaskConstants.TASK_ORDER_PROPERTY)]
            if (taskOrder == null || taskOrder.asDouble() != position) {
                return task.copy(
                    original = task.original ?: task.deepCopy(),
                    dataviewFields = task.dataviewFields.plus(DataviewField(TaskConstants.TASK_ORDER_PROPERTY) to DataviewValue(position)).toDataviewMap()
                )
            }
            return task
        }
    }
}
