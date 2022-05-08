package neurallink.core.service

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mu.KotlinLogging
import neurallink.core.model.Task
import neurallink.core.store.*

private val logger = KotlinLogging.logger("TaskFilterFunctions")

fun filterTasks(tasks: List<Task>, filterValue: FilterValue<out Any>) : List<Task> {
    return when (filterValue) {
        is NoneFilterValue -> tasks
        is TagFilterValue -> tasks.filter { task -> task.tags.contains(filterValue.filterValue) }
        is FileFilterValue -> tasks.filter { task -> task.file == filterValue.filterValue }
        is DataviewFilterValue -> tasks.filter { task ->
            task.dataviewFields.containsKey(filterValue.filterValue.value.first)
            // TODO This is only filtering on the key being present, not using the value at all
        }
        is FutureDateFilterValue -> {
            // TODO Is TimeZone here going to affect anything?
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                .let { LocalDate(it.year, it.month, it.dayOfMonth) }
                .let {
                    logger.debug { "Filtering using $it" }
                    it
                }
                .let { date ->
                    tasks.filter {
                        if (filterValue.filterValue) {
                            if (it.dueOn == null) true else it.dueOn <= date
                        } else {
                            true
                        }
                    }
                }
        }
    }
}