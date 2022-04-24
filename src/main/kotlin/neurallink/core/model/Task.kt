package neurallink.core.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.uuid.UUID

@Suppress("NON_EXPORTABLE_TYPE", "EXPERIMENTAL_IS_NOT_ENABLED") // List is flagged for this but is valid
@OptIn(ExperimentalJsExport::class)
@JsExport
data class Task(
    val file: TaskFile,
    override val filePosition: FilePosition,
    val description: Description,
    val dueOn: DueOn?,
    val completedOn: CompletedOn?,
    val tags: Set<Tag>,
    val dataviewFields: DataviewMap,
    val completed: Boolean,
    val subtasks: List<Task> = listOf(),
    val notes: List<Note> = listOf(),
    val original: Task? = null, // TODO try to automate setting this
    // 'before' is for writing the repeat task
    // TODO Find a better way to model this as I don't like needing to store this on the task itself
    val before: Task? = null,
    val id: TaskId = TaskId(UUID())
) : ListItem() {
    @OptIn(ExperimentalSerializationApi::class)
    fun deepCopy(): Task {
        return this.copy(
            tags = tags.map { tag -> tag.copy() }.toMutableSet(),
            dataviewFields = dataviewFields.copy(),
            subtasks = subtasks.map { subtask -> subtask.deepCopy() }.toMutableList(),
            notes = notes.map { note -> note.copy() }.toMutableList()
        )
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
        markdownElements.add(description.value)
        if (tags.isNotEmpty()) {
            markdownElements.add(tags.joinToString(" ") { tag -> "#${tag.value}" })
        }
        if (dataviewFields.isNotEmpty()) {
            markdownElements.add(dataviewFields.map { (key, value) -> "[${key.value}:: ${value.value}]" }.joinToString("  "))
        }
        if (dueOn != null) {
            markdownElements.add("@due(${dueOn.value})")
        }
        if (completedOn != null) {
            markdownElements.add("@completed(${completedOn.value})")
        }
        if (subtasks.isNotEmpty()) {
            markdownElements.add("\n\t" + subtasks.joinToString("\n\t") { it.toMarkdown() })
        }
        if (notes.isNotEmpty()) {
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
