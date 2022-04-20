package neurallink.core.model

import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class Note(
    val note: String,
    override val filePosition: Int,
    val subnotes: List<Note> = listOf()
) : ListItem()
