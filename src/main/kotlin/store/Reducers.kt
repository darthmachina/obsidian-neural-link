package store

import kotlinx.datetime.LocalDate
import model.StatusTag
import model.Task
import model.TaskConstants
import model.TaskModel
import org.reduxkotlin.Reducer
import service.RepeatingTaskService

val reducerFunctions = Reducers()

val reducer: Reducer<TaskModel> = { store, action ->
    when (action) {
        is VaultLoaded -> reducerFunctions.copyAndPopulateKanban(store, action.tasks)
        is TaskStatusChanged -> reducerFunctions.taskStatusChanged(store, action.taskId, action.newStatus, action.beforeTask)
        is ModifyFileTasks -> reducerFunctions.modifyFileTasks(store, action.file, action.fileTasks, action.repeatingTaskService)
        is TaskCompleted -> reducerFunctions.taskCompleted(store, action.taskId)
        is SubtaskCompleted -> reducerFunctions.markSubtaskCompletion(store, action.taskId, action.subtaskId, action.complete)
        is RepeatTask -> store
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
            console.log(" - columns updated, reloading kanban")
            val clonedTaskList = store.tasks.map { it.deepCopy() }
            store.copy(
                settings = newSettings,
                tasks = clonedTaskList,
                kanbanColumns = ReducerUtils.createKanbanMap(clonedTaskList, newSettings.columnTags)
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
        return store.copy(tasks = tasks, kanbanColumns = ReducerUtils.createKanbanMap(tasks, store.settings.columnTags))
    }

    fun taskStatusChanged(store: TaskModel, taskId: String, newStatus: String, beforeTaskId: String?): TaskModel {
        console.log("Reducers.taskStatusChanged()")
        val clonedTaskList = store.tasks.map { it.deepCopy() }

        // First, find the current status column
        val movedTask = clonedTaskList.find { it.id == taskId }
        if (movedTask == null) {
            console.log("ERROR: Did not find task for id: $taskId")
        } else {
            movedTask.tags.remove(ReducerUtils.getStatusTagFromTask(movedTask, store.settings.columnTags)?.tag)
            movedTask.tags.add(newStatus)
            if (beforeTaskId == null) {
                // No before task, add to end of list
                movedTask.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = ReducerUtils.findMaxPosition(clonedTaskList, newStatus).toString()
            } else {
                val beforeTaskPos = clonedTaskList.find { it.id == beforeTaskId }!!.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]!!.toInt()
                movedTask.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = beforeTaskPos.toString()
                clonedTaskList
                    .filter { task -> task.tags.contains(newStatus) }
                    .sortedBy { task -> task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] }
                    .forEach { task ->
                        // Moved task already has the right position, increment all pos that are after it
                        val taskPos = task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toInt() ?: 0
                        if (beforeTaskPos <= taskPos) {
                            ReducerUtils.updateTaskOrder(movedTask, taskPos + 1)
                        }
                    }
            }
        }
        return store.copy(tasks = clonedTaskList, kanbanColumns = ReducerUtils.createKanbanMap(clonedTaskList, store.settings.columnTags))
    }

    fun taskCompleted(store: TaskModel, taskId: String): TaskModel {
        console.log("Reducers.taskCompleted()")
        val clonedTaskList = store.tasks.map { it.deepCopy() }
        val task = clonedTaskList.find { task -> task.id == taskId }
        if (task == null) {
            console.log(" - ERROR: Task not found for id: $taskId")
            return store
        }
        ReducerUtils.setModifiedIfNeeded(task)
        task.completed = true
        return store.copy(tasks = clonedTaskList, kanbanColumns = ReducerUtils.createKanbanMap(clonedTaskList, store.settings.columnTags))
    }

    fun markSubtaskCompletion(store: TaskModel, taskId: String, subtaskId: String, complete: Boolean): TaskModel {
        console.log("Reducers.subtaskCompleted()")
        val clonedTaskList = store.tasks.map { it.deepCopy() }
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
        return store.copy(tasks = clonedTaskList)
    }

    fun modifyFileTasks(store: TaskModel, file: String, fileTasks: List<Task>, repeatingTaskService: RepeatingTaskService): TaskModel {
        console.log("Reducers.modifyFileTasks()")
        val clonedTaskList = store.tasks
            .map { it.deepCopy() }
            .filter { it.file != file }
            .plus(fileTasks)
        ReducerUtils.runFileModifiedListeners(fileTasks, store.settings.columnTags, repeatingTaskService)

        return store.copy(tasks = clonedTaskList, kanbanColumns = ReducerUtils.createKanbanMap(clonedTaskList, store.settings.columnTags))
    }
}

class ReducerUtils {
    companion object {
        private val taskComparator = compareBy<Task,Int?>(nullsLast()) {
            val position = it.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toInt()
            position
        }

        private val taskDateComparator = compareBy<Task,LocalDate?>(nullsFirst()) {
            it.dueOn
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
            return tasks.mapIndexed { index, task ->
                if (task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] == null) {
                    updateTaskOrder(task, index)
                } else {
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

        fun findMaxPosition(tasks: List<Task>, status: String) : Int {
            return tasks
                .filter { task -> task.tags.contains(status) }
                .sortedWith(compareBy(nullsLast()) { task -> task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] })
                .last()
                .dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]!!.toInt()
        }

        fun runFileModifiedListeners(tasks: List<Task>, statusTags: List<StatusTag>, repeatingTaskService: RepeatingTaskService) {
            console.log("runFileModifiedListeners()")

            // Repeating tasks
            tasks
                .filter { task ->
                    task.dataviewFields.keys.contains(TaskConstants.TASK_REPEAT_PROPERTY) &&
                            task.completed
                }
                .forEach { task ->
                    repeatTask(task, repeatingTaskService)
                }

            // Check for completed tasks with a status tag and remove the tag (might have been completed outside the app)
            tasks
                .filter { it.completed && getStatusTagFromTask(it, statusTags) != null}
                .forEach { task ->
                    setModifiedIfNeeded(task)
                    task.tags.remove(getStatusTagFromTask(task, statusTags)!!.tag)
                }
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
//            console.log("Reducers.ReducerUtils.getStatusTagFromTask()", task)
            val statusColumn = kanbanKeys.filter { statusTag -> task.tags.contains(statusTag.tag) }
            if (statusColumn.size > 1) {
                console.log("ERROR: More than one status column is on the task: ", statusColumn)
                return null
            } else if (statusColumn.size == 1) {
                return statusColumn[0]
            }
//        console.log("ERROR: status tag not found on task: ", task)
            return null
        }

        /**
         * Updates the task order for a task if required (order is null or already set to the given value), saving the
         * original before making the change.
         */
        fun updateTaskOrder(task: Task, position: Int): Task {
            console.log("updateTaskOrder()", task, position)
            val taskOrder = task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]
//        console.log(" - current task order", taskOrder)
            if (taskOrder == null || taskOrder.toInt() != position) {
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
