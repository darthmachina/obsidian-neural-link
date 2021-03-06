package neurallink.core.model

@OptIn(ExperimentalJsExport::class)
@JsExport
data class Note(
    val note: String,
    override val filePosition: FilePosition,
    val subnotes: List<Note> = listOf()
) : ListItem() {
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
