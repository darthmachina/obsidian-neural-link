package store

import model.Task
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
    /**
     * Called when the vault is initially loaded with a task list, will populate the kanban data
     */
    fun vaultLoaded(newTaskModel: TaskModel): TaskModel {
        console.log("vaultLoaded()")
        val columnTags = newTaskModel.settings.columnTags
        newTaskModel.tasks.forEach { task ->
            val statusColumn = task.tags.filter { it in columnTags }
            if (statusColumn.size > 1) {
                console.log("ERROR: More than one status column is on the task: ", statusColumn)
            } else if (statusColumn.size == 1) {
                newTaskModel.kanbanColumns[statusColumn[0]]?.add(task)
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
        updatedKanbanColumns.keys.forEach { status ->
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
                    statusTasks.add(statusTasks.indexOf(beforeTasks[0]), task)
                }
                task.tags.add(newStatus)
            }
        }

        return taskModel.copy(tasks = updatedTaskList, kanbanColumns = updatedKanbanColumns)
    }

    fun taskCompleted() {

    }

    private fun moveTaskStatus(taskModel: TaskModel, task: Task, oldStatus: String, newStatus: String) {

    }
}
