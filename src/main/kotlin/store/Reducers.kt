package store

import model.StatusTag
import model.Task
import model.TaskConstants
import model.TaskModel
import org.reduxkotlin.Reducer

val reducerFunctions = Reducers()

val reducer: Reducer<TaskModel> = { store, action ->
    when (action) {
        is VaultLoaded -> reducerFunctions.vaultLoaded(action.newTaskModel)
        is TaskStatusChanged -> reducerFunctions.taskStatusChanged(store, action.taskId, action.newStatus, action.beforeTask)
        is ModifyFileTasks -> reducerFunctions.modifyFileTasks(store, action.file, action.fileTasks)
        is TaskCompleted -> reducerFunctions.taskCompleted(store, action.taskId)
        is SubtaskCompleted -> reducerFunctions.subtaskCompleted(store, action.taskId, action.subtaskId)
        is UpdateSettings -> reducerFunctions.updateSettings(store, action)
        else -> store
    }
}

class Reducers {
    private val taskComparator = compareBy<Task,Int?>(nullsLast()) {
        val position = it.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]?.toInt()
        console.log(" - position for Task '${it.description}' : $position")
        position
    }

    /**
     * Updates the settings, if any update value is null it will reuse the value in the store to allow for partial
     * updates.
     */
    fun updateSettings(store: TaskModel, updateSettings: UpdateSettings): TaskModel {
        console.log("updateSettings()")
        // If the columns have changed we need to reprocess the kanbanColumns data
        var newKanbanColumns: MutableMap<String,MutableList<Task>>? = null
        if (updateSettings.columnTags != null) {
            newKanbanColumns = createKanbanColumns(updateSettings.columnTags!!)
            insertTasksIntoKanban(newKanbanColumns, store.tasks)
        }

        val updatedTaskModel = store.copy(
            settings = store.settings.copy(
                taskRemoveRegex = updateSettings.taskRemoveRegex ?: store.settings.taskRemoveRegex,
                columnTags = updateSettings.columnTags ?: store.settings.columnTags
            ),
            kanbanColumns = newKanbanColumns ?: store.kanbanColumns
        )
        updateSettings.plugin.saveData(updateSettings.settingsService.toJson(updatedTaskModel.settings))
        return updatedTaskModel
    }

    /**
     * Called when the vault is initially loaded with a task list, will populate the kanban data
     */
    fun vaultLoaded(newTaskModel: TaskModel): TaskModel {
        console.log("vaultLoaded()")
        // Insert tasks sorted by TASK_ORDER to maintain any previously saved order into the kanban
        val filteredTasks = newTaskModel.tasks.filter { task -> task.tags.any { tag -> tag in newTaskModel.kanbanColumns.keys } }
        insertTasksIntoKanban(newTaskModel.kanbanColumns, filteredTasks)

        return newTaskModel
    }

    fun taskStatusChanged(store: TaskModel, taskId: String, newStatus: String, beforeTaskId: String?): TaskModel {
        console.log("taskStatusChanged()")
        val newTaskModel = copyTasksIntoNewModel(store)
        val updatedTaskList = newTaskModel.tasks
        val updatedKanbanColumns = newTaskModel.kanbanColumns

        // First, find the current status column
        val filteredTasks = updatedTaskList.filter { it.id == taskId }
        if (filteredTasks.isEmpty() || filteredTasks.size > 1) {
            console.log("ERROR: Did not find just one task for id: $taskId")
        } else {
            val task = filteredTasks[0]
            // Remove the task from that column
            updatedKanbanColumns.keys.forEach taskLoop@{ status ->
                if (updatedKanbanColumns[status]!!.contains(task)) {
                    removeAndUpdateTaskOrder(task, updatedKanbanColumns[status]!!, status)
                    return@taskLoop
                }
            }
            // Add the task to the list for the new status
            // If beforeTasksId is set add it at the same position (so pushes beforeTask down), else just add to the end
            if (updatedKanbanColumns.keys.contains(newStatus)) {
                val beforeTasks = updatedTaskList.filter { it.id == beforeTaskId }
                val statusTasks = updatedKanbanColumns[newStatus]!!
                if (beforeTasks.isEmpty() || beforeTasks.size > 1) {
                    console.log(" - Did not find just one task to place before, adding to bottom")
                    statusTasks.add(updateTaskOrder(task, statusTasks.size))
                } else {
                    val beforeTaskIndex = statusTasks.indexOf(beforeTasks[0])
                    if (beforeTaskIndex == -1) {
                        // Not found, log it and just add to the end of the list
                        console.log("ERROR: Task $beforeTaskId not found in status $newStatus, adding to end of list")
                        statusTasks.add(updateTaskOrder(task, statusTasks.size))
                    } else {
                        insertAndUpdateTaskOrder(task, statusTasks, beforeTaskIndex)
                        console.log(" - tasks after calling insertAndUpdateTaskOrder : ", statusTasks)
                    }
                }
                task.tags.add(newStatus)
            }
        }

        return store.copy(tasks = updatedTaskList, kanbanColumns = updatedKanbanColumns)
    }

    fun taskCompleted(store: TaskModel, taskId: String): TaskModel {
        console.log("taskCompleted()")
        val newTaskModel = copyTasksIntoNewModel(store)
        val task = newTaskModel.tasks.find { task -> task.id == taskId }
        if (task == null) {
            console.log(" - ERROR: Task not found for id: $taskId")
            return store
        }
        setModifiedIfNeeded(task)
        task.completed = true
        val statusColumn = getStatusTagFromTask(task, newTaskModel.kanbanColumns.keys)
        if (statusColumn != null) {
            removeAndUpdateTaskOrder(task, newTaskModel.kanbanColumns[statusColumn]!!, statusColumn)
        }
        // task is already in the task list so just return the new model
        return newTaskModel
    }

    fun subtaskCompleted(store: TaskModel, taskId: String, subtaskId: String): TaskModel {
        console.log("subtaskCompleted()")
        val newTaskModel = copyTasksIntoNewModel(store)
        val task = newTaskModel.tasks.find { task -> task.id == taskId }
        if (task == null) {
            console.log(" - ERROR: Task not found for id: $taskId")
            return store
        }
        val subtask = task.subtasks.find { subtask -> subtask.id == subtaskId }
        if (subtask == null) {
            console.log(" - ERROR: Subtask not found in Task $taskId for subtask id $subtaskId")
            return store
        }

        setModifiedIfNeeded(task)
        subtask.completed = true
        // task is already in the task list so just return the new model
        return newTaskModel
    }

    fun modifyFileTasks(store: TaskModel, file: String, fileTasks: List<Task>): TaskModel {
        console.log("modifyFileTasks()")
        val newTaskModel = copyTasksIntoNewModel(store)
        clearFileTasksFromModel(newTaskModel, file)
        newTaskModel.tasks.addAll(fileTasks)
        insertTasksIntoKanban(newTaskModel.kanbanColumns, fileTasks)

        return newTaskModel
    }

    private fun createKanbanColumns(statusTags: List<StatusTag>): MutableMap<String,MutableList<Task>> {
        console.log("createKanbanColumns()", statusTags)
        val kanbanColumns = mutableMapOf<String,MutableList<Task>>()
        statusTags.forEach { statusTag ->
            kanbanColumns[statusTag.tag] = mutableListOf()
        }
        return kanbanColumns
    }

    /**
     * Inserts the tasks given into the correct column in kanbanColumns.
     *
     * NOTE: kanbanColumns must contain keys for every status in use already
     * SIDE EFFECT: kanbanColumns is modified in place
     */
    private fun insertTasksIntoKanban(kanbanColumns: MutableMap<String,MutableList<Task>>, tasks: List<Task>) {
        tasks.sortedWith(taskComparator).forEach { task ->
            val statusColumn = getStatusTagFromTask(task, kanbanColumns.keys)
            if (statusColumn != null) {
                val statusTasks = kanbanColumns[statusColumn]!!
                statusTasks.add(task)
                updateTaskOrder(task, statusTasks.indexOf(task))
            }
        }
    }

    private fun getStatusTagFromTask(task: Task, kanbanKeys: MutableSet<String>): String? {
        val statusColumn = task.tags.filter { tag -> tag in kanbanKeys }
        if (statusColumn.size > 1) {
            console.log("ERROR: More than one status column is on the task: ", statusColumn)
            return null
        } else if (statusColumn.size == 1) {
            return statusColumn[0]
        }
        console.log("ERROR: status tag not found on task: ", task)
        return null
    }

    /**
     * Creates a new TaskModel with updated tasks and kanbanColumns collections containing cloned tasks.
     */
    private fun copyTasksIntoNewModel(store: TaskModel): TaskModel {
        val updatedTaskList = store.tasks.map { it.deepCopy() }.toMutableList()
        val updatedKanbanColumns = mutableMapOf<String,MutableList<Task>>()
        store.kanbanColumns.keys.forEach { status ->
            updatedKanbanColumns[status] = mutableListOf()
            updatedKanbanColumns[status]!!.addAll(store.kanbanColumns[status]!!.map { statusTask ->
                // Find the cloned task in the task list to put in the column map
                updatedTaskList.find { task -> task.id == statusTask.id }!!
            })
        }

        return store.copy(tasks = updatedTaskList, kanbanColumns = updatedKanbanColumns)
    }

    /**
     * Removes all tasks from the store that are from the given file.
     *
     * SIDE EFFECT: taskModel.tasks is updated in place
     */
    private fun clearFileTasksFromModel(taskModel: TaskModel, file: String) {
        console.log("clearFileTasksFromModel()")
        val fileTasks = taskModel.tasks.filter { it.file == file }
        console.log(" - remove tasks in file from task list:", fileTasks)
        taskModel.tasks.removeAll(fileTasks)
        console.log(" - remove tasks in file from kanban columns")
        taskModel.kanbanColumns.forEach { entry ->
            entry.value.removeAll { it.file == file }
        }
    }

    /**
     * Inserts the given task into the task list at position.
     *
     * NOTE: Side effect: tasks is mutated directly
     */
    private fun insertAndUpdateTaskOrder(task: Task, tasks: MutableList<Task>, position: Int) {
        console.log("insertAndUpdateTaskOrder(task, tasks, $position)")
        // Move Order for each task
        for (i in position until tasks.size) {
            val newOrder = tasks[i].dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]!!.toInt() + 1
            console.log(" - newOrder for Task '${tasks[i].description}' is $newOrder")
            updateTaskOrder(tasks[i], newOrder)
        }
        // Incoming task gets the position
        tasks.add(updateTaskOrder(task, position))
        tasks.sortWith(taskComparator)
        console.log(" - tasks after sorting : ", tasks)
    }

    /**
     * Removes the given task from the task list and updates the order of all tasks below it.
     *
     * @param task The task to remove from the task list
     * @param tasks Task list for a single status assumed to already be sorted by position
     * @param status The old status of the task
     */
    private fun removeAndUpdateTaskOrder(task: Task, tasks: MutableList<Task>, status: String) {
        console.log("removeAndUpdateTaskOrder()")
        val taskIndex = tasks.indexOf(task)
        if (taskIndex == -1) {
            console.log("ERROR: task not found in task list", task)
            return
        }
        setModifiedIfNeeded(task)
        tasks.remove(task)
        task.dataviewFields.remove(TaskConstants.TASK_ORDER_PROPERTY)
        task.tags.remove(status)
        for (i in taskIndex until tasks.size) {
            updateTaskOrder(tasks[i], i)
        }
    }

    /**
     * Updates the task order for a task if required (order is null or already set to the given value), saving the
     * original before making the change.
     */
    private fun updateTaskOrder(task: Task, position: Int): Task {
        console.log("updateTaskOrder()", task, position)
        val taskOrder = task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY]
        if (taskOrder == null || taskOrder.toInt() != position) {
            console.log(" - task order needs to be updated : $taskOrder -> $position")
            setModifiedIfNeeded(task)
            task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = position.toString()
        }
        return task
    }

    /**
     * Saves the original task if needed (if it has not already been set for a different modification).
     */
    private fun setModifiedIfNeeded(task: Task) {
        console.log("setModifiedIfNeeded()", task)
        if (task.original == null) task.original = task.deepCopy()
    }
}
