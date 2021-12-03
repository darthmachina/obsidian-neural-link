package service

import kotlin.js.Date

@OptIn(ExperimentalJsExport::class)
@JsExport
class TaskService {
    private val dueDateRegex = Regex("""@due\(([0-9\-T:]*)\)""")

    /**
     * Gets a date from the task string tagged with the given tag.
     *
     * So, given `Task @due(2021-01-01T00:00:00)`, this will return a Date object set to`2021-01-01T00:00:00` when asking for the `due` tag
     *
     * TODO Support Dataview style inline fields
     *
     * @param task The task String
     * @return A Date object representing the due date or null if no due date is present
     */
    fun getDueDateFromTask(task: String) : Date? {
        val dateMatch = dueDateRegex.find(task)
        console.log("dateMatch: ${dateMatch?.groupValues?.get(1)}")
        return dateMatch?.let {
            return Date(dateMatch.groupValues[1])
        } ?: return null
    }
}
