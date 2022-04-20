package neurallink.core.model

object TaskConstants {
    val DUE_ON_PROPERTY = DataviewField("due")
    val COMPLETED_ON_PROPERTY = DataviewField("completed")
    val TASK_ORDER_PROPERTY = DataviewField("pos")
    val TASK_REPEAT_PROPERTY = DataviewField("repeat")

    enum class REPEAT_SPAN(val tag: String) {
        DAILY("daily"),
        WEEKLY("weekly"),
        MONTHLY("monthly"),
        YEARLY("yearly"),
        WEEKDAY("weekday"),
        MONTH("month"),
        JANUARY("jan"),
        FEBRUARY("feb"),
        MARCH("mar"),
        APRIL("apr"),
        MAY("may"),
        JUNE("jun"),
        JULY("jul"),
        AUGUST("aug"),
        SEPTEMBER("sep"),
        OCTOBER("oct"),
        NOVEMBER("nov"),
        DECEMBER("dec"),
        UNKNOWN("unknown");

        companion object {
            fun getAllTags(): List<String> {
                return values().map { it.tag }
            }

            fun findForTag(tag: String): REPEAT_SPAN? {
                return values().find { it.tag == tag}
            }
        }
    }

    const val TASK_PAPER_DATE_FORMAT = """\(([0-9\-T:]*)\)"""
    const val ALL_TAGS_REGEX = """#([a-zA-Z][0-9a-zA-Z-_/]*)"""
    const val DATAVIEW_REGEX = """\[([a-zA-Z]*):: ([\d\w!: -.]*)\]"""
    @Suppress("RegExpRedundantEscape")
    const val COMPLETED_REGEX = """- \[[xX]\]"""

    val spanRegex = REPEAT_SPAN.getAllTags().joinToString("|")
    val repeatItemRegex = Regex("""($spanRegex)([!]?)(: ([0-9]{1,2}))?""")
}
