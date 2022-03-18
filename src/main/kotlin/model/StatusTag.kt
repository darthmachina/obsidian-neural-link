package model

import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class StatusTag(val tag: String, val displayName: String)
