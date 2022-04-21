package neurallink.core.model

import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class StatusTag(val tag: Tag, val displayName: String, val dateSort: Boolean = false)
