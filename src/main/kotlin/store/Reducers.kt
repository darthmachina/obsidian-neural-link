package store

import kotlinx.datetime.LocalDate
import model.*
import org.reduxkotlin.Reducer
import service.RepeatingTaskService

val reducerFunctions = Reducers()

val reducer: Reducer<TaskModel> = { store, action ->
    when (action) {
        is VaultLoaded -> reducerFunctions.copyAndPopulateKanban(store, action.tasks)
        is TaskMoved -> reducerFunctions.moveCard(store, action.taskId, action.newStatus, action.beforeTask)
        is MoveToTop -> reducerFunctions.moveToTop(store, action.taskd)
        is ModifyFileTasks -> reducerFunctions.modifyFileTasks(store, action.file, action.fileTasks, action.repeatingTaskService)
        is TaskCompleted -> reducerFunctions.taskCompleted(store, action.taskId, action.repeatingTaskService)
        is SubtaskCompleted -> reducerFunctions.markSubtaskCompletion(store, action.taskId, action.subtaskId, action.complete)
        is RepeatTask -> store
        is FilterByTag -> reducerFunctions.filterByTag(store, action.tag)
        is FilterByFile -> reducerFunctions.filterByFile(store, action.file)
        is FilterByDataviewValue -> reducerFunctions.filterByDataviewValue(store, action.value)
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
        updateSettings.plugin.saveData(updateSettings.settingsService.toJson(newSettings))
        return if (updateSettings.columnTags != null) {
//            console.log(" - columns updated, reloading kanban")
            val clonedTaskList = store.tasks.map { it.deepCopy() }
            store.copy(
                settings = newSettings,
                tasks = clonedTaskList,
                kanbanColumns = ReducerUtils.createKanbanMap(
                    ReducerUtils.filterTasks(clonedTaskList, store.filterType, store.filterValue),
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

    fun moveCard(store: TaskModel, taskId: String, newStatus: String, beforeTaskId: String?): TaskModel {
        console.log("Reducers.taskStatusChanged()")
        val clonedTaskList = store.tasks.map { it.deepCopy() }
        val movedTask = clonedTaskList.find { it.id == taskId }
        if (movedTask == null) {
            console.log(" - ERROR: Did not find task for id: $taskId")
        } else {
            ReducerUtils.setModifiedIfNeeded(movedTask)
            val oldStatus = ReducerUtils.getStatusTagFromTask(movedTask, store.settings.columnTags)!!
            movedTask.tags.remove(oldStatus.tag)
            ReducerUtils.updateTaskOrder(movedTask, ReducerUtils.findPosition(clonedTaskList, newStatus, beforeTaskId))
            movedTask.tags.add(newStatus)
        }
        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterType, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun moveToTop(store: TaskModel, taskId: String) : TaskModel {
        val clonedTaskList = store.tasks.map { it.deepCopy() }
        val movedTask = clonedTaskList.find { it.id == taskId }
        if (movedTask == null) {
            console.log(" - ERROR: Did not find task for id: $taskId")
            return store
        }

        val newPosition = clonedTaskList
            .filter { task -> task.tags.contains(ReducerUtils.getStatusTagFromTask(movedTask, store.settings.columnTags)?.tag) }
            .sortedWith(compareBy(nullsLast()) { task -> task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toDouble() })
            .first()
            .dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]!!.toDouble() / 2
        ReducerUtils.setModifiedIfNeeded(movedTask)
        movedTask.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = newPosition.toString()
        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterType, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun taskCompleted(store: TaskModel, taskId: String, repeatingTaskService: RepeatingTaskService): TaskModel {
        console.log("Reducers.taskCompleted()")
        val clonedTaskList = store.tasks.map { it.deepCopy() }
        console.log(" - store and cloned list", store, clonedTaskList)
        val task = clonedTaskList.find { task -> task.id == taskId }
        if (task == null) {
            console.log(" - ERROR: Task not found for id: $taskId")
            return store
        }
        ReducerUtils.completeTask(task, store.settings.columnTags, repeatingTaskService)
        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterType, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun markSubtaskCompletion(store: TaskModel, taskId: String, subtaskId: String, complete: Boolean): TaskModel {
        console.log("Reducers.subtaskCompleted()")
        val clonedTaskList = store.tasks.map { it.deepCopy() }
        console.log(" - store and cloned list", store, clonedTaskList)
        val task = clonedTaskList.find { task -> task.id == taskId }
        if (task == null) {
            console.log(" - ERROR: Task not found for id: $taskId")
            return store
        }
        val subtask = task.subtasks.find { subtask -> subtask.id == subtaskId }
        if (subtask == null) {
            console.log(" - ERROR: Subtask not found in Task $taskId for subtask id $subtaskId")
            return store
        }

        ReducerUtils.setModifiedIfNeeded(task)
        subtask.completed = complete
        return store.copy(
            tasks = clonedTaskList,
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(clonedTaskList, store.filterType, store.filterValue),
                store.settings.columnTags
            )
        )
    }

    fun modifyFileTasks(store: TaskModel, file: String, fileTasks: List<Task>, repeatingTaskService: RepeatingTaskService): TaskModel {
        console.log("Reducers.modifyFileTasks()")
        ReducerUtils.runFileModifiedListeners(fileTasks, store, repeatingTaskService)
        // Only return a new state if any of the tasks were modified
        console.log(" - checking to see if the store needs to be updated")
        if (fileTasks.any { it.original != null } || ReducerUtils.changedTasks(file, fileTasks, store).isNotEmpty()) {
            console.log(" - yes, updating store")
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

        return store
    }

    /**
     * Filters the task list according to the given tag; a null tag means there should be no filter.
     */
    fun filterByTag(store: TaskModel, tag: String?) : TaskModel {
        val filterType = if (tag == null) FilterType.NONE else FilterType.TAG
        val filterValue = tag ?: ""
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, filterType, filterValue),
                store.settings.columnTags
            ),
            filterType = filterType,
            filterValue = filterValue
        )
    }

    fun filterByFile(store: TaskModel, file: String?) : TaskModel {
        val filterType = if (file == null) FilterType.NONE else FilterType.FILE
        val filterValue = file ?: ""
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, filterType, filterValue),
                store.settings.columnTags
            ),
            filterType = filterType,
            filterValue = filterValue
        )
    }

    fun filterByDataviewValue(store: TaskModel, value: String?) : TaskModel {
        val filterType = if (value == null) FilterType.NONE else FilterType.DATAVIEW
        val filterValue = value ?: ""
        return store.copy(
            kanbanColumns = ReducerUtils.createKanbanMap(
                ReducerUtils.filterTasks(store.tasks, filterType, filterValue),
                store.settings.columnTags
            ),
            filterType = filterType,
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
            it.dueOn
        }

        fun filterTasks(tasks: List<Task>, filterType: FilterType, filterValue: String) : List<Task> {
            return when (filterType) {
                FilterType.NONE -> tasks
                FilterType.TAG -> tasks.filter { task -> task.tags.contains(filterValue) }
                FilterType.FILE -> tasks.filter { task -> task.file == filterValue }
                FilterType.DATAVIEW -> tasks.filter { task ->
                    val dataview = filterValue.split("::")
                    task.dataviewFields.containsKey(dataview[0]) && task.dataviewFields[dataview[0]] == dataview[1]
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
         * Finds the position for a task, calculated given the following scenarios:
         *
         * 1. No tasks for the status, returns 1.0 (to leave room before it for other cards)
         * 2. No beforeTaskId given, returns the max position value + 1 to put it at the end
         * 3. beforeTaskId given, find that task and the one before it and returns a value in the middle of the two pos values
         *  - If beforeTask is the first in the list just return its position divided by 2
         */
        fun findPosition(tasks: List<Task>, status: String, beforeTaskId: String? = null) : Double {
            console.log("ReducerUtils.findPosition()")
            return if (tasks.none { task -> task.tags.contains(status) }) {
//                console.log(" - list is empty, returning 1.0")
                1.0
            } else if (beforeTaskId == null) {
//                console.log(" - no beforeTaskId, adding to end of list")
                (tasks
                    .filter { task -> task.tags.contains(status) }
                    .sortedWith(compareBy(nullsLast()) { task -> task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toDouble() })
                    .last()
                    .dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]!!.toDouble()) + 1.0
            } else {
//                console.log(" - beforeTaskId set, finding new position")
                val statusTasks = tasks
                    .filter { task -> task.tags.contains(status) }
                    .sortedWith(compareBy(nullsLast()) { task -> task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toDouble() })
                val beforeTask = statusTasks.find { it.id == beforeTaskId }
                    ?: throw IllegalStateException("beforeTask not found for id $beforeTaskId")
                val beforeTaskPosition = beforeTask.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toDouble()
                    ?: throw IllegalStateException("beforeTask does not have a position property")
                val beforeTaskIndex = statusTasks.indexOf(beforeTask)
                // Returns new position
                if (beforeTaskIndex == 0) {
                    beforeTaskPosition / 2
                } else {
                    val beforeBeforeTask = statusTasks[beforeTaskIndex - 1]
                    val beforeBeforeTaskPosition = beforeBeforeTask.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toDouble()
                        ?: throw IllegalStateException("beforeBeforeTask does not have a position property")
                    (beforeTaskPosition + beforeBeforeTaskPosition) / 2
                }
            }
        }

        /**
         * Returns a list of Tasks from a file that have changed from within the store
         */
        fun changedTasks(file: String, fileTasks: List<Task>, store: TaskModel) : List<Task> {
            // Take the fileTasks list and subtrack any that are equal to what is already in the store
            val storeFileTasks = store.tasks.filter { it.file == file }
            if (storeFileTasks.isEmpty()) return emptyList()

            console.log("ReducerUtils.changedTasks()", fileTasks, storeFileTasks)
            return fileTasks.minus(storeFileTasks.toSet())
        }

        fun runFileModifiedListeners(tasks: List<Task>, store: TaskModel, repeatingTaskService: RepeatingTaskService) {
            console.log("Reducers.ReducerUtils.runFileModifiedListeners()", tasks)

            // Check for completed tasks with either a status tag or a repeat field (might have been completed outside the app)
            // remove the status tag and check for any tasks that need repeating
            tasks
                .filter { task ->
                    task.completed &&
                            (getStatusTagFromTask(task, store.settings.columnTags) != null
                                    || task.dataviewFields.keys.contains(TaskConstants.TASK_REPEAT_PROPERTY))
                }
                .forEach { task -> completeTask(task, store.settings.columnTags, repeatingTaskService) }

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

        fun completeTask(task: Task, columns: Collection<StatusTag>, repeatingTaskService: RepeatingTaskService) {
            setModifiedIfNeeded(task)
            repeatTask(task, repeatingTaskService)
            task.completed = true
            task.dataviewFields.remove(TaskConstants.TASK_ORDER_PROPERTY)
            task.tags.removeAll { tag -> tag in columns.map { it.tag } }
        }

        /**
         * Repeats the given task if required.
         *
         * Sets the task.before field to the repeated task to write the new task before the current one in the file.
         */
        private fun repeatTask(task: Task, repeatingTaskService: RepeatingTaskService) {
            console.log("repeatTask()", task)
            if (repeatingTaskService.isTaskRepeating(task)) {
//            console.log(" - task is a repeating task, processing")
                val repeatTask = repeatingTaskService.getNextRepeatingTask(task)
                setModifiedIfNeeded(task)
                task.dataviewFields.remove(TaskConstants.TASK_REPEAT_PROPERTY)
                task.before = repeatTask
            }
        }

        fun getStatusTagFromTask(task: Task, kanbanKeys: Collection<StatusTag>): StatusTag? {
            console.log("Reducers.ReducerUtils.getStatusTagFromTask()", task)
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
//        console.log(" - current task order", taskOrder)
            if (taskOrder == null || taskOrder.toDouble() != position) {
//            console.log(" - task order needs to be updated : $taskOrder -> $position")
                setModifiedIfNeeded(task)
                task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = position.toString()
            }
            return task
        }

        /**
         * Saves the original task if needed (if it has not already been set for a different modification).
         */
        fun setModifiedIfNeeded(task: Task) {
            console.log("setModifiedIfNeeded()", task)
            if (task.original == null) task.original = task.deepCopy()
        }
    }
}
