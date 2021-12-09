package model

/**
 * Model for a Task.
 *
 * Contains the following data:
 * - original : The original task description, unprocessed
 * - description : The text of the task (no tags, metadata, etc)
 * - tags : The tags on the task (either `#` or `@`)
 * - metadata : The Dataview fields on the task
 * - subtasks : List of indented tasks under this task
 * - notes : Indented list items that aren't tasks
 */
data class Task(
    val original: String,
    val description: String,
    val tags: List<String>,
    val metadata: List<String>,
    val subtasks: List<Task>,
    val notes: List<String>
)
