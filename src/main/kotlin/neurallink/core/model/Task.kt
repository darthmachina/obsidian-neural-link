package neurallink.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.uuid.UUID

@Suppress("NON_EXPORTABLE_TYPE", "EXPERIMENTAL_IS_NOT_ENABLED") // List is flagged for this but is valid
@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class Task(
    val file: String,
    override val filePosition: Int,
    val description: String,
    val dueOn: LocalDate?,
    val completedOn: LocalDateTime?,
    val tags: Set<String>,
    val dataviewFields: Map<String, String>,
    val completed: Boolean,
    val subtasks: List<Task> = mutableListOf(),
    val notes: List<Note> = mutableListOf(),
    val original: Task? = null, // TODO try to automate setting this
    // 'before' is for writing the repeat task
    // TODO Find a better way to model this as I don't like needing to store this on the task itself
    val before: Task? = null,
    val id: String = UUID().toString()
) : ListItem() {
    /**
     * Compares just the relevant data fields for e
     */
    override fun equals(other: Any?) : Boolean {
        return other is Task &&
                id == other.id &&
                file == other.file &&
                description == other.description &&
                dueOn == other.dueOn &&
                tags == other.tags &&
                dataviewFields == other.dataviewFields &&
                completed == other.completed &&
                subtasks == other.subtasks &&
                notes == other.notes
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + (dueOn?.hashCode() ?: 0)
        result = 31 * result + tags.hashCode()
        result = 31 * result + dataviewFields.hashCode()
        result = 31 * result + completed.hashCode()
        result = 31 * result + subtasks.hashCode()
        result = 31 * result + notes.hashCode()
        return result
    }
}
