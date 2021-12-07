package service

import kotlin.js.Date

@OptIn(ExperimentalJsExport::class)
@JsExport
class TaskService {
    private val dueDateRegex = Regex("""@due\(([0-9\-T:]*)\)""")
    private val recurRegex = Regex("""(daily|weekly|monthly|yearly|month|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)([!]?): ([0-9]{1,2})""")

    fun parseRecurring(recurText: String) : RecurItem {
        val matches = recurRegex.find(recurText)
        console.log("matches: ", matches)
        if (matches?.groupValues == null) {
            return RecurItem("ERROR", "", false, 0)
        }

        var index = 1
        val type = matches?.groupValues?.get(index++)!!
        val fromComplete = matches.groupValues[index++] == "!"
        val amount = matches.groupValues[index]!!.toInt()

        var recurType: String
        if (type in listOf("daily", "weekly", "monthly", "yearly")) {
            recurType = "SPAN"
        } else {
            recurType = "SPECIFIC"
        }

        return RecurItem(recurType, type, fromComplete, amount)
    }

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

@OptIn(ExperimentalJsExport::class)
@JsExport
data class RecurItem(val type: String, val span: String, val fromComplete: Boolean, val amount: Int)