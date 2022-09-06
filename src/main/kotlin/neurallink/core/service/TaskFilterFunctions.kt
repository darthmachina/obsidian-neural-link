package neurallink.core.service

import arrow.core.Some
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mu.KotlinLogging
import neurallink.core.model.FilterOptions
import neurallink.core.model.Task
import neurallink.core.store.*

private val logger = KotlinLogging.logger("TaskFilterFunctions")

/**
 * Checks if the given path is in one of the given paths or is a child of the given path
 */
fun pathInPathList(path: String, paths: List<String>) : Boolean {
    return paths.map { onePath ->
            path.startsWith(onePath, ignoreCase = true)
        }
        .fold(false) { acc, next ->
            acc || next
        }
}

fun filterTasks(tasks: List<Task>, filters: FilterOptions) : List<Task> {
    return tasks.filter { task ->
        filters.tags.fold({true}, { tagFilter -> task.tags.contains(tagFilter.filterValue) })
                && filters.page.fold({true}, {pageFilter -> task.file == pageFilter.filterValue})
                && filters.dataview.fold({true}, {dataviewFilter -> task.dataviewFields.containsKey(dataviewFilter.filterValue.value.first)})
                && Some(filters.hideFuture).fold({false}, {futureFilter ->
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                .let { LocalDate(it.year, it.month, it.dayOfMonth) }
                .let { date ->
                    if (futureFilter) {
                        if (task.dueOn == null) true else task.dueOn <= date
                    } else {
                        true
                    }
                }

        })
    }
}
