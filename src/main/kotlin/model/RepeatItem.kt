package model

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
data class RepeatItem(
    val span: TaskConstants.REPEAT_SPAN,
    val fromComplete: Boolean,
    val amount: Int
)
