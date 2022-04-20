package neurallink.core.service

import neurallink.core.model.Note
import neurallink.core.model.Task

fun Task.toMarkdown(): String {
    val markdownElements = mutableListOf<String>()

    markdownElements.add(if (this.completed) "- [x]" else "- [ ]")
    markdownElements.add(this.description.value)
    if (this.tags.isNotEmpty()) {
        markdownElements.add(this.tags.joinToString(" ") { tag -> "#$tag" })
    }
    if (this.dataviewFields.isNotEmpty()) {
        markdownElements.add(this.dataviewFields.map { (key, value) -> "[$key:: $value]" }.joinToString("  "))
    }
    if (this.dueOn != null) {
        markdownElements.add("@due(${this.dueOn})")
    }
    if (this.completedOn != null) {
        markdownElements.add("@completed(${this.completedOn})")
    }
    if (this.subtasks.isNotEmpty()) {
        markdownElements.add("\n\t" + this.subtasks.joinToString("\n\t") { it.toMarkdown() })
    }
    if (this.notes.isNotEmpty()) {
        markdownElements.add("\n\t" + this.notes.joinToString("\n\t") { note -> note.toMarkdown(1) })
    }

    // Check for a 'before' task
    var beforeMarkdown = ""
    if (this.before != null) {
        beforeMarkdown = "${this.before.toMarkdown()}\n"
    }
    return beforeMarkdown + markdownElements.joinToString(" ")
}

fun Note.toMarkdown(level: Int) : String {
    var markdown = "- $note"

    if (this.subnotes.isNotEmpty()) {
        val nextLevel = level + 1
        val separator = "\n" + "\t".repeat(nextLevel)
        markdown += this.subnotes.joinToString(separator) { subnote -> subnote.toMarkdown(nextLevel) }
    }

    return markdown;
}


