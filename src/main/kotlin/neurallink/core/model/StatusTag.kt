package neurallink.core.model

import kotlinx.serialization.Serializable
import service.StatusTagSerializer

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable(with = StatusTagSerializer::class)
data class StatusTag(val tag: Tag, val displayName: String, val dateSort: Boolean = false) {
    companion object {
        fun fromTripe(triple: Triple<String, String, Boolean>) : StatusTag {
            return StatusTag(Tag(triple.first), triple.second, triple.third)
        }
    }

    fun toTriple() : Triple<String,String,Boolean> {
        return Triple(tag.value, displayName, dateSort)
    }
}
