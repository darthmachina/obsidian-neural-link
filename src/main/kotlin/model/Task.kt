package model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.uuid.UUID

@Suppress("NON_EXPORTABLE_TYPE", "EXPERIMENTAL_IS_NOT_ENABLED") // List is flagged for this but is valid
@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class Task(
    val file: String,
    val filePosition: Int,
    var description: String,
    var dueOn: SimpleDate?,
    var completedOn: SimpleDate?,
    val tags: MutableSet<String>,
    val dataviewFields: MutableMap<String, String>,
    var completed: Boolean,
    val subtasks: MutableList<Task> = mutableListOf(),
    val notes: MutableList<String> = mutableListOf(),
    val id: UUID = UUID()
) {
    @OptIn(ExperimentalSerializationApi::class)
    fun deepCopy(): Task {
        val bytes = Cbor.encodeToByteArray(this)
        return Cbor.decodeFromByteArray(bytes)
    }

    /**
     * Creates a Markdown String, suitable for writing to a Markdown file.
     *
     * Does not indent itself, but will recursively call this on any subtasks, applying
     * indentation where needed to maintain the hierarchy.
     */
    fun toMarkdown(): String {
        val completedMarker = if (completed) "x" else " "
        val markdownTags = if (tags.size > 0) {
            " " + tags.joinToString(" ") { tag -> "#$tag" }
        } else {
            ""
        }
        val markdownDataview = if (dataviewFields.isNotEmpty()) {
            " " + dataviewFields.map { (key, value) -> "[$key:: $value]" }.joinToString(" ")
        } else {
            ""
        }
        val markdownDue = dueOn?.toMarkdown("due") ?: ""
        val markdownCompleted = completedOn?.toMarkdown("completed", true) ?: ""
        val markdownSubtasks = if (subtasks.size > 0) {
            "\n\t" + subtasks.joinToString("\n\t") { it.toMarkdown() }
        } else {
            ""
        }
        val markdownNotes = if (notes.size > 0) {
            "\n\t" + notes.joinToString("\n\t") { note -> "- $note" }
        } else {
            ""
        }
        return "- [$completedMarker] $description $markdownDataview $markdownTags $markdownDue $markdownCompleted $markdownSubtasks $markdownNotes"
    }
}
