package model

import kotlinx.serialization.Serializable

@Suppress("NON_EXPORTABLE_TYPE", "EXPERIMENTAL_IS_NOT_ENABLED") // List is flagged for this but is valid
@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class SimpleDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0
) {
    fun toMarkdown(tag: String, includeTime: Boolean = false): String {
        val time = if (includeTime) "T$hour:$minute:$second" else ""
        return "@$tag($year-$month-$day$time)"
    }

    override fun toString(): String {
        return "$year-$month-$day"
    }
}

