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
        is ModifyFileTasks -> store
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
        newTaskModel.tasks
            // First, sort by TASK_ORDER to maintain any previously saved order
            .sortedWith(taskComparator)
            // Now add the tasks into the appropriate status column, adding a TASK_ORDER value if it doesn't exist
            .forEach { task ->
                val statusColumn = task.tags.filter { it in columnTags }
                if (statusColumn.size > 1) {
                    console.log("ERROR: More than one status column is on the task: ", statusColumn)
                } else if (statusColumn.size == 1) {
                    val statusTasks = newTaskModel.kanbanColumns[statusColumn[0]]!!
                    statusTasks.add(task)
                    if (task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] == null) {
                        task.original = task.deepCopy()
                        task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = statusTasks.indexOf(task).toString()
                    }
                } // Don't care about size == 0
            }

        return newTaskModel
    }

    fun taskStatusChanged(taskModel: TaskModel, taskId: String, newStatus: String, beforeTaskId: String?): TaskModel {
        console.log("taskStatusChanged()")
        val updatedTaskList = taskModel.tasks.map { it.deepCopy() }.toMutableList()
        val updatedKanbanColumns = mutableMapOf<String,MutableList<Task>>()
        taskModel.kanbanColumns.keys.forEach { status ->
            updatedKanbanColumns[status] = mutableListOf()
            updatedKanbanColumns[status]!!.addAll(taskModel.kanbanColumns[status]!!.map { it.deepCopy() })
        }

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
                if (beforeTasks.isEmpty() || beforeTasks.size > 1) {
                    console.log("Did not find just one task to place before, adding to bottom")
                    updatedKanbanColumns[newStatus]!!.add(task)
                } else {
                    val statusTasks = updatedKanbanColumns[newStatus]!!
                    val beforeTaskIndex = statusTasks.indexOf(beforeTasks[0])
                    if (beforeTaskIndex == -1) {
                        // Not found, log it and just add to the end of the list
                        console.log("ERROR: Task $beforeTaskId not found in status $newStatus, adding to end of list")
                        statusTasks.add(task)
                    } else {
                        insertAndUpdateTaskOrder(task, statusTasks, beforeTaskIndex)
                    }
                }
                task.tags.add(newStatus)
            }
        }

        return taskModel.copy(tasks = updatedTaskList, kanbanColumns = updatedKanbanColumns)
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
            tasks[i].original = tasks[i].deepCopy()
            tasks[i].dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = newOrder.toString()
        }
        // Incoming task gets the position
        task.original = task.deepCopy()
        task.dataviewFields[TaskConstants.TASK_ORDER_PROPERTY] = position.toString()
        tasks.add(task)
        tasks.sortWith(taskComparator)
        console.log(" - tasks after sorting : ", tasks)
    }
}
