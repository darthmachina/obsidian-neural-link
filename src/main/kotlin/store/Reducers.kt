package store

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
        is TaskCompleted -> store
        is UpdateSettings -> store.copy(settings = action.newSettings)
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
     * Called when the vault is initially loaded with a task list, will populate the kanban data
     */
    fun vaultLoaded(newTaskModel: TaskModel): TaskModel {
        console.log("vaultLoaded()")
        val columnTags = newTaskModel.settings.columnTags
        // Insert tasks sorted by TASK_ORDER to maintain any previously saved order into the kanban
        val filteredTasks = newTaskModel.tasks.filter { task -> task.tags.any { it in newTaskModel.kanbanColumns.keys } }
        insertTasksIntoKanban(newTaskModel.kanbanColumns, filteredTasks.sortedWith(taskComparator))

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
                    updatedKanbanColumns[status]!!.remove(task)
                    task.tags.remove(status)
                    return@taskLoop
                }
            }
            // Add the task to the list for the new status
            // If beforeTasksId is set add it at the same position (so pushes beforeTask down), else just add to the end
            if (updatedKanbanColumns.keys.contains(newStatus)) {
                val beforeTasks = updatedTaskList.filter { it.id == beforeTaskId }
                val statusTasks = updatedKanbanColumns[newStatus]!!
                if (beforeTasks.isEmpty() || beforeTasks.size > 1) {
                    console.log("Did not find just one task to place before, adding to bottom")
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

    fun modifyFileTasks(store: TaskModel, file: String, fileTasks: List<Task>): TaskModel {
        console.log("modifyFileTasks()")
        val newTaskModel = copyTasksIntoNewModel(store)
        clearFileTasksFromModel(newTaskModel, file)
        newTaskModel.tasks.addAll(fileTasks)
        insertTasksIntoKanban(newTaskModel.kanbanColumns, fileTasks)

        return newTaskModel
    }

    /**
     * Inserts the tasks given into the correct column in kanbanColumns.
     *
     * NOTE: kanbanColumns must contain keys for every status in use already
     * SIDE EFFECT: kanbanColumns is modified in place
     */
    private fun insertTasksIntoKanban(kanbanColumns: MutableMap<String,MutableList<Task>>, tasks: List<Task>) {
        tasks.forEach { task ->
            val statusColumn = task.tags.filter { it in kanbanColumns.keys }
            if (statusColumn.size > 1) {
                console.log("ERROR: More than one status column is on the task: ", statusColumn)
            } else if (statusColumn.size == 1) {
                val statusTasks = kanbanColumns[statusColumn[0]]!!
                statusTasks.add(task)
                if (task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] == null) {
                    updateTaskOrder(task, statusTasks.indexOf(task))
                }
            } // Don't care about size == 0
        }
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
     * Inserts the given task into the task list at position.sudo pacman -S archlinux-keyring
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
     * Updates the task order for a task, saving the original before making the change
     */
    private fun updateTaskOrder(task: Task, position: Int): Task {
        task.original = getCopyIfNeeded(task)
        task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = position.toString()
        return task
    }

    /**
     * Gets a copy of the task if required (it has not already been set by a different modification), otherwise just
     * returns the original Task that was already set.
     */
    private fun getCopyIfNeeded(task: Task): Task {
        return if (task.original == null) task.deepCopy() else task.original!!
    }
}
