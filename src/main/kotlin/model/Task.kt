package model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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
    var dueOn: LocalDate?,
    var completedOn: LocalDateTime?,
    val tags: MutableSet<String>,
    val dataviewFields: MutableMap<String, String>,
    var completed: Boolean,
    val subtasks: MutableList<Task> = mutableListOf(),
    val notes: MutableList<Note> = mutableListOf(),
    var original: Task? = null, // TODO try to automate setting this
    // 'before' is for writing the repeat task
    // TODO Find a better way to model this as I don't like needing to store this on the task itself
    var before: Task? = null,
    val id: String = UUID().toString()
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
        val markdownElements = mutableListOf<String>()

        markdownElements.add(if (completed) "- [x]" else "- [ ]")
        markdownElements.add(description)
        if (tags.size > 0) {
            markdownElements.add(tags.joinToString(" ") { tag -> "#$tag" })
        }
        if (dataviewFields.isNotEmpty()) {
            markdownElements.add(dataviewFields.map { (key, value) -> "[$key:: $value]" }.joinToString("  "))
        }
        if (dueOn != null) {
            markdownElements.add("@due(${dueOn!!})")
        }
        if (completedOn != null) {
            markdownElements.add("@completed(${completedOn!!})")
        }
        if (subtasks.size > 0) {
            markdownElements.add("\n\t" + subtasks.joinToString("\n\t") { it.toMarkdown() })
        }
        if (notes.size > 0) {
            markdownElements.add("\n\t" + notes.joinToString("\n\t") { note -> note.toMarkdown(1) })
        }

        // Check for a 'before' task
        var beforeMarkdown = ""
        if (before != null) {
            beforeMarkdown = "${before!!.toMarkdown()}\n"
        }
        return beforeMarkdown + markdownElements.joinToString(" ")
    }

    /**
     * Compares just the relevant data fields for e
     */
    override fun equals(other: Any?) : Boolean {
        return other is Task &&
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
        var result = file.hashCode()
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
