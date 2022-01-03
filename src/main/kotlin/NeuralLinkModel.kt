import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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
@Suppress("NON_EXPORTABLE_TYPE", "EXPERIMENTAL_IS_NOT_ENABLED") // List is flagged for this but is valid
@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class Task(
    val full: String,
    val description: String,
    @Serializable(with = DateAsDoubleSerializer::class)
    var due: Date?, // Moment format yyyy-MM-DD
    @Serializable(with = DateAsDoubleSerializer::class)
    var completedDate: Date?, // Moment format yyyy-MM-DDTHH:mm:ss
    val tags: MutableList<String>, // TODO Need a Tag class?
    val dataviewFields: MutableMap<String,String>,
    var completed: Boolean,
    val subtasks: MutableList<Task> = mutableListOf(),
    val notes: MutableList<String> = mutableListOf()
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
        val completedMarker = if (completed) "X" else " "
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
        val markdownDue = if (due == null) "" else " @due(${moment.utc(due).format("yyyy-MM-DD")})"
        val markdownCompleted = if (completedDate == null) "" else " @completed(${moment.utc(completedDate).format("yyyy-MM-DDTHH:mm:ss")})"
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
        return "- [$completedMarker] $description$markdownDataview$markdownTags$markdownDue$markdownCompleted$markdownSubtasks$markdownNotes"
    }
}

object DateAsDoubleSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor= PrimitiveSerialDescriptor("Date", PrimitiveKind.DOUBLE)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeDouble())
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeDouble(value.getTime())
}

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
data class RepeatItem(val type: String, val span: String, val fromComplete: Boolean, val amount: Int)

data class ModifiedTask(var original: Task, val before: MutableList<Task> = mutableListOf(), val after: MutableList<Task> = mutableListOf(), var modified: Boolean = false)
