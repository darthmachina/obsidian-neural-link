import kotlin.js.Date

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
    val due: Date?, // Moment format yyyy-MM-DD
    val tags: List<String>, // TODO Need a Tag class?
    val dataviewFields: List<DataviewField>,
    val subtasks: MutableList<Task> = mutableListOf(),
    val notes: MutableList<String> = mutableListOf()
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class DataviewField(
    val key: String,
    val value: String
)
