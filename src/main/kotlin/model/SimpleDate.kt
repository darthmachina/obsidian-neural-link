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
    val second: Int = 0,
    var includeTime: Boolean = false
) {
    fun toMarkdown(tag: String): String {
        return "@$tag(${toString()})"
    }

    override fun toString(): String {
        val monthPadded = month.toString().padStart(2, '0')
        val dayPadded = day.toString().padStart(2, '0')
        val hourPadded = hour.toString().padStart(2, '0')
        val minutePadded = minute.toString().padStart(2, '0')
        val secondPadded = second.toString().padStart(2, '0')
        val time = if (includeTime) "T$hourPadded:$minutePadded:$secondPadded" else ""
        return "$year-$monthPadded-$dayPadded$time"
    }
}

