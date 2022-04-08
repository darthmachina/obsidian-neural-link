package model

import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class Note(val note: String, val subnotes: List<Note> = mutableListOf()) {
    fun toMarkdown(level: Int) : String {
        var markdown = "- $note"

        if (subnotes.isNotEmpty()) {
            val nextLevel = level + 1
            val separator = "\n" + "\t".repeat(nextLevel)
            markdown += subnotes.joinToString(separator) { subnote -> subnote.toMarkdown(nextLevel) }
        }

        return markdown;
    }
}
