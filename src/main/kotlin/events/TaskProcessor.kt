package events

/**
 * Interface for doing something to a task.
 *
 * Right now the plan is that only main tasks will get passed into the processor.
 */
interface TaskProcessor {
    /**
     * Performs some work on a task and returns the updated task.
     */
    fun processTask(task: String) : String
}