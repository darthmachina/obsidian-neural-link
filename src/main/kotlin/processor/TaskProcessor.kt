package processor

/**
 * Interface for doing something to a task.
 *
 * TODO Right now only main tasks are passed into the `processTask` method. That will be expanded to include all
 *  indented items in the future.
 */
interface TaskProcessor {
    /**
     * Performs some work on a task and returns the updated task.
     */
    fun processTask(task: String): String

    fun getPriority() : Int
}
