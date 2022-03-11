package model

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
data class RepeatItem(
    val type: String,
    val span: String,
    val fromComplete: Boolean,
    val amount: Int
)
