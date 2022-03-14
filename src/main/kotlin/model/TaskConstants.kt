package model

object TaskConstants {
    const val DUE_ON_PROPERTY = "due"
    const val COMPLETED_ON_PROPERTY = "completed"
    const val TASK_ORDER_PROPERTY = "pos"

    enum class REPEATING_TYPE(val tag: String) {
        DAILY("daily"),
        WEEKLY("weekly"),
        MONTHLY("monthly"),
        YEARLY("yearly"),
        WEEKDAY("weekday");

        companion object {
            fun getAllTags(): List<String> {
                return values().map { it.tag }
            }
        }
    }

    enum class SPECIFIC_INSTANTS(val tag: String) {
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
        DECEMBER("dec");

        companion object {
            fun getAllTags(): List<String> {
                return values().map { it.tag }
            }
        }
    }
}
