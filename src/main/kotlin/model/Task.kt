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
@Suppress("NON_EXPORTABLE_TYPE") // List is flagged for this but is valid
@OptIn(ExperimentalJsExport::class)
@JsExport
data class Task(
    val original: String,
    val description: String,
    val tags: List<String>, // TODO Need a Tag class?
    val metadata: List<String>, // TODO Create DataviewField class
    val subtasks: List<Task>,
    val notes: List<String>
)
