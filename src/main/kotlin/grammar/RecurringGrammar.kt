package grammar

/**
 * Not a true grammar for now, just some RegEx to pull out relevant data
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class RecurringGrammar {
    private val recurRegex = Regex("""(daily|weekly|monthly|yearly|month|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)([!]?): ([0-9]{1,2})""")

    fun parse(recurText: String) : RecurItem{
        val matches = recurRegex.find(recurText)
        console.log("matches: ", matches)

        var index = 1
        val type = matches?.groupValues?.get(index++)!!
        val fromComplete = matches?.groupValues?.get(index++) == "!"
        val amount = matches?.groupValues?.get(index)!!.toInt()

        return RecurItem(type, fromComplete, amount)
    }
}

data class RecurItem(val type: String, val fromComplete: Boolean, val amount: Int)