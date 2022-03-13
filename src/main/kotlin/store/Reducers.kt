package store

import model.Task
import model.TaskModel
import org.reduxkotlin.Reducer
import org.reduxkotlin.Store

val reducerFunctions = Reducers()

val reducer: Reducer<TaskModel> = { store, action ->
    when (action) {
        is VaultLoaded -> reducerFunctions.vaultLoaded(action.newTaskModel)
        is TaskStatusChanged -> {
            console.log("Action: TaskStatusChanged")
            val taskModel = reducerFunctions.taskStatusChanged(store, action.taskId, action.newStatus)
            taskModel.kanbanColumns.keys.forEach { status ->
                console.log(" - task list for $status : ", taskModel.kanbanColumns[status]!!)
            }
            taskModel
        }
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

    fun taskStatusChanged(taskModel: TaskModel, taskId: String, newStatus: String): TaskModel {
        console.log("taskStatusChaged()")
        val updatedTaskList = taskModel.tasks.map { it.deepCopy() }.toMutableList()
        val updatedKanbanColumns = mutableMapOf<String,MutableList<Task>>()
        taskModel.kanbanColumns.keys.forEach { status ->
            console.log(" - task list for $status : ", taskModel.kanbanColumns[status]!!)
            console.log("  - copied task list : ", taskModel.kanbanColumns[status]!!.map { it.deepCopy() })
            updatedKanbanColumns[status] = mutableListOf()
            updatedKanbanColumns[status]!!.addAll(taskModel.kanbanColumns[status]!!.map { it.deepCopy() })
        }
        console.log(" ---")
        updatedKanbanColumns.keys.forEach { status ->
            console.log(" - task list for $status : ", updatedKanbanColumns[status]!!)
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
                    console.log(" - found task in column $status")
                    updatedKanbanColumns[status]!!.remove(task)
                    task.tags.remove(status)
                    return@taskLoop
                }
            }
            // Add the task to the bottom of the list for the new status
            if (updatedKanbanColumns.keys.contains(newStatus)) {
                console.log(" - adding task to newStatus $newStatus")
                updatedKanbanColumns[newStatus]!!.add(task)
                task.tags.add(newStatus)
            }
        }

        updatedKanbanColumns.keys.forEach { status ->
            console.log(" - creating new task list for $status : ", updatedKanbanColumns[status]!!)
        }
        val newTaskModel = TaskModel(
            taskModel.settings,
            updatedTaskList,
            updatedKanbanColumns
        )
        newTaskModel.kanbanColumns.keys.forEach { status ->
            console.log(" - final task list for $status : ", newTaskModel.kanbanColumns[status]!!)
        }
        return newTaskModel
    }

    fun taskCompleted() {

    }

    private fun moveTaskStatus(taskModel: TaskModel, task: Task, oldStatus: String, newStatus: String) {

    }
}
