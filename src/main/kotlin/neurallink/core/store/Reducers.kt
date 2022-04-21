package neurallink.core.store

import arrow.core.getOrElse
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import model.*
import neurallink.core.model.*
import neurallink.core.service.addTag
import neurallink.core.service.filterTags
import neurallink.core.service.getOriginal
import neurallink.core.store.*
import neurallink.view.*
import org.reduxkotlin.Reducer

val reducerFunctions = Reducers()

val reducer: Reducer<TaskModel> = { store, action ->
    when (action) {
        is VaultLoaded -> reducerFunctions.copyAndPopulateKanban(store, action.tasks)
        is TaskMoved -> reducerFunctions.moveCard(store, action.taskId, action.newStatus, action.beforeTask)
        is MoveToTop -> reducerFunctions.moveToTop(store, action.taskId)
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
            columnTags = updateSettings.columnTags ?: store.settings.columnTags
        )
        // FIXME Side effect
        updateSettings.plugin.saveData(updateSettings.settingsService.toJson(newSettings))
        return if (updateSettings.columnTags != null) {
            store.copy(
                settings = newSettings,
                kanbanColumns = ReducerUtils.createKanbanMap(
                    ReducerUtils.filterTasks(store.tasks, store.filterType, store.filterValue),
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
                ReducerUtils.filterTasks(tasks, store.filterType, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun moveCard(store: TaskModel, taskId: TaskId, newStatus: Tag, beforeTaskId: String?): TaskModel {
        console.log("Reducers.taskStatusChanged()")
        val clonedTaskList = store.tasks.map { task ->
            if (task.id == taskId) {
                // Change status tags
                val oldStatus = ReducerUtils.getStatusTagFromTask(task, store.settings.columnTags)!!
                task.copy(
                    original = getOriginal(task),
                    tags = filterTags(task.tags) {
                        it != oldStatus.tag
                    }.apply {
                        addTag(this, newStatus)
                    },
                    dataviewFields = setTaskOrder(task.dataviewFields, findPosition(store.tasks, newStatus, beforeTaskId))
                )
            } else {
                task
            }
        }
        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterType, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun moveToTop(store: TaskModel, taskId: TaskId) : TaskModel {
        val clonedTaskList = store.tasks.map { task ->
            if (task.id == taskId) {
                val original = getOriginal(task)
                task.copy(
                    original = original,
                    dataviewFields = setTaskOrder(
                        task.dataviewFields,
                        firstTaskPosition(
                            store.tasks.filterTasksByStatusTag(
                                findStatusTag(task.tags, store.settings.columnTags)
                                    .getOrElse { StatusTag(Tag(""), "") } // TODO Handle an error here
                            )
                        )
                    )
                )
            } else {
                task
            }
        }

        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterType, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun taskCompleted(
        store: TaskModel,
        taskId: TaskId,
        subtaskChoice: IncompleteSubtaskChoice
    ): TaskModel {
        console.log("Reducers.taskCompleted()")
        val clonedTaskList = store.tasks.map { task ->
            if (task.id == taskId) {
                val original = task.deepCopy()
                task.deepCopy().copy(original = original, subtasks = newSubtasks)
            } else {
                task
            }
        }

        console.log(" - store and cloned list", store, clonedTaskList)
        ReducerUtils.completeTask(task, subtaskChoice, store.settings.columnTags)
        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterType, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun markSubtaskCompletion(store: TaskModel, taskId: TaskId, subtaskId: TaskId, complete: Boolean): TaskModel {
        console.log("Reducers.subtaskCompleted()")
        val clonedTaskList = store.tasks.map { task ->
            if (task.id == taskId) {
                val newSubtasks = task.subtasks.map { subtask ->
                    if (subtask.id == subtaskId) {
                        subtask.copy(completed = complete)
                    } else {
                        subtask
                    }
                }
                val original = task.deepCopy()
                task.deepCopy().copy(original = original, subtasks = newSubtasks)
            } else {
                task
            }
        }

        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterType, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun modifyFileTasks(store: TaskModel, file: TaskFile, fileTasks: List<Task>): TaskModel {
        console.log("Reducers.modifyFileTasks()")
        ReducerUtils.runFileModifiedListeners(fileTasks, store)
        // Only return a new state if any of the tasks were modified
        val clonedTaskList = store.tasks
            .map { it.deepCopy() }
            .filter { it.file != file }
            .plus(fileTasks)

        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterType, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    /**
     * Filters the task list according to the given tag; a null tag means there should be no filter.
     */
    fun filterByTag(store: TaskModel, tag: Tag?) : TaskModel {
        val filterType = if (tag == null) FilterType.NONE else FilterType.TAG
        val filterValue = TagFilterValue(tag ?: Tag(""))
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, filterType, filterValue),
                store.settings.columnTags
            ),
            filterType = filterType,
            filterValue = filterValue
        )
    }

    fun filterByFile(store: TaskModel, file: TaskFile?) : TaskModel {
        val filterType = if (file == null) FilterType.NONE else FilterType.FILE
        val filterValue = FileFilterValue(file ?: TaskFile(""))
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, filterType, filterValue),
                store.settings.columnTags
            ),
            filterType = filterType,
            filterValue = filterValue
        )
    }

    fun filterByDataviewValue(store: TaskModel, value: DataviewPair?) : TaskModel {
        val filterType = if (value == null) FilterType.NONE else FilterType.DATAVIEW
        val filterValue = DataviewFilterValue(value ?: DataviewPair(DataviewField("") to DataviewValue("")))
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, filterType, filterValue),
                store.settings.columnTags
            ),
            filterType = filterType,
            filterValue = filterValue
        )
    }

    fun filterFutureDate(store: TaskModel, filter: Boolean) : TaskModel {
        val filterValue = FutureDateFilterValue(filter)
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, FilterType.CURRENT_DATE, filterValue),
                store.settings.columnTags
            ),
            filterType = FilterType.CURRENT_DATE,
            filterValue = filterValue
        )
    }
}

class ReducerUtils {
    companion object {
        private val taskComparator = compareBy<Task,Double?>(nullsLast()) {
            val position = it.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toDouble()
            position
        }

        private val taskDateComparator = compareBy<Task,LocalDate?>(nullsFirst()) {
            it.dueOn?.value
        }

        fun filterTasks(tasks: List<Task>, filterType: FilterType, filterValue: FilterValue<out Any>) : List<Task> {
            return when (filterType) {
                FilterType.NONE -> tasks
                FilterType.TAG -> tasks.filter { task -> task.tags.contains(filterValue.filterValue) }
                FilterType.FILE -> tasks.filter { task -> task.file == filterValue.filterValue }
                FilterType.DATAVIEW -> tasks.filter { task ->
                    val dataview = filterValue.split("::")
                    task.dataviewFields.containsKey(DataviewField(dataview[0])) && task.dataviewFields.valueForField(dataview[0]) == dataview[1]
                }
                FilterType.CURRENT_DATE -> {
                    val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) // TODO Is TimeZone here going to affect anything?
                    val currentDate = LocalDate(currentDateTime.year, currentDateTime.month, currentDateTime.dayOfMonth)
                    console.log("Filtering using $currentDate")
                    tasks.filter {
                        if (filterValue.filterValue) {
                            if (it.dueOn == null) true else it.dueOn.value <= currentDate
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
                if (task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] == null) {
                    updateTaskOrder(task, maxPosition++)
                } else {
                    maxPosition = task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]!!.toDouble()
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
         * Returns a list of Tasks from a file that have changed from within the store
         */
        fun changedTasks(file: String, fileTasks: List<Task>, store: TaskModel) : List<Task> {
            // Take the fileTasks list and subtrack any that are equal to what is already in the store
            val storeFileTasks = store.tasks.filter { it.file == TaskFile(file) }
            if (storeFileTasks.isEmpty()) return emptyList()

            console.log("ReducerUtils.changedTasks()", fileTasks, storeFileTasks)
            return fileTasks.minus(storeFileTasks.toSet())
        }

        fun runFileModifiedListeners(tasks: List<Task>, store: TaskModel) {
            console.log("Reducers.ReducerUtils.runFileModifiedListeners()", tasks)

            // Check for completed tasks with either a status tag or a repeat field (might have been completed outside the app)
            // remove the status tag and check for any tasks that need repeating. Do nothing with subtasks as we are outside
            // the app.
            tasks
                .filter { task ->
                    task.completed &&
                            (getStatusTagFromTask(task, store.settings.columnTags) != null
                                    || task.dataviewFields.keys.contains(TaskConstants.TASK_REPEAT_PROPERTY))
                }
                .forEach { task -> completeTask(task, IncompleteSubtaskChoice.NOTHING, store.settings.columnTags, repeatingTaskService) }

            // Check for tasks with no position
            tasks
                .filter { task ->
                    val statusTag = getStatusTagFromTask(task, store.settings.columnTags)
                    task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] == null &&
                            statusTag != null &&
                            !statusTag.dateSort
                }
                .forEach { task ->
                    setModifiedIfNeeded(task)
                    task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = findPosition(
                        store.tasks,
                        getStatusTagFromTask(task, store.settings.columnTags)!!.tag).toString()
                }
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
            val taskOrder = task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]
            if (taskOrder == null || taskOrder.toDouble() != position) {
                setModifiedIfNeeded(task)
                task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = position
            }
            return task
        }

        /**
         * Saves the original task if needed (if it has not already been set for a different modification).
         */
        fun setModifiedIfNeeded(task: Task) {
            console.log("setModifiedIfNeeded()", task)
        }
    }
}
