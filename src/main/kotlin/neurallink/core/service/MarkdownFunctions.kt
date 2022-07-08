package neurallink.core.service

import neurallink.core.model.Task

val WIKILINK_REGEX = Regex("""\[\[([A-Za-z ]+)\]\]""")
val BOLD_REGEX = Regex("""\*\*([A-Za-z ]+)\*\*""")
val ITALIC_REGEX = Regex("""\*([A-Za-z <>"=/-]+)\*""")

/**
 * Creates a Markdown String, suitable for writing to a Markdown file.
 *
 * Does not indent itself, but will recursively call this on any subtasks, applying
 * indentation where needed to maintain the hierarchy.
 */
fun toMarkdown(task: Task): String {
    val markdownElements = mutableListOf<String>()

    markdownElements.add(if (task.completed) "- [x]" else "- [ ]")
    markdownElements.add(task.description.value)
    if (task.tags.isNotEmpty()) {
        markdownElements.add(task.tags.joinToString(" ") { tag -> "#${tag.value}" })
    }
    if (task.dataviewFields.isNotEmpty()) {
        markdownElements.add(task.dataviewFields.map { (key, value) -> "[${key.value}:: ${value.value}]" }.joinToString("  "))
    }
    if (task.dueOn != null) {
        markdownElements.add("@due(${task.dueOn.value})")
    }
    if (task.completedOn != null) {
        markdownElements.add("@completed(${task.completedOn.value})")
    }
    if (task.subtasks.isNotEmpty()) {
        markdownElements.add("\n\t" + task.subtasks.joinToString("\n\t") { toMarkdown(it) })
    }
    if (task.notes.isNotEmpty()) {
        markdownElements.add("\n\t" + task.notes.joinToString("\n\t") { note -> note.toMarkdown(1) })
    }

    // Check for a 'before' task
    var beforeMarkdown = ""
    if (task.before != null) {
        beforeMarkdown = "${toMarkdown(task.before)}\n"
    }
    return beforeMarkdown + markdownElements.joinToString(" ")
}
