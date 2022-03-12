package store

import model.TaskModel
import org.reduxkotlin.Reducer

val reducerFunctions = Reducers()

val reducer: Reducer<TaskModel> = { store, action ->
    when (action) {
        is VaultLoaded -> reducerFunctions.vaultLoaded(action.newTaskModel)
        is ModifyFileTasks -> store
        is TaskCompleted -> store
        is UpdateSettings -> store.copy(settings = action.newSettings)
        else -> store
    }
}

class Reducers {
    fun vaultLoaded(newTaskModel: TaskModel): TaskModel {
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

    fun taskCompleted() {

    }
}
