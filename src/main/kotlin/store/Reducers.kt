package store

import model.TaskModel
import org.reduxkotlin.Reducer

val reducer: Reducer<TaskModel> = { state, action ->
    when (action) {
        is VaultLoaded -> action.newTaskModel
        is ModifyFileTasks -> state
        is TaskCompleted -> state
        is UpdateSettings -> state
        else -> state
    }
}

class Reducers {
}
