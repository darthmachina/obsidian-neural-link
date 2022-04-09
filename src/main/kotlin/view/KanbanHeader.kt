package view

import io.kvision.core.*
import io.kvision.form.select.SimpleSelect
import io.kvision.form.select.SimpleSelectInput
import io.kvision.form.select.simpleSelect
import io.kvision.form.select.simpleSelectInput
import io.kvision.html.div
import io.kvision.panel.HPanel
import io.kvision.utils.perc
import io.kvision.utils.px
import model.TaskModel
import org.reduxkotlin.Store
import store.FilterByTag

class KanbanHeader(val store: Store<TaskModel>) : HPanel(spacing = 10, justify = JustifyContent.END) {
    init {
        addCssStyle(KanbanStyles.KANBAN_HEADER)
        div { +"Filters" }
        div { +"Tag: " }
        simpleSelectInput(getAllTags(), emptyOption = true) {
            placeholder = "Select a tag"
            width = 150.px
            background = Background(color = Color.name(Col.BLACK))
            color = Color.name(Col.WHITE)

//            style("select") {
//                width = 70.perc
//            }

            var init = true
            subscribe {
//                console.log("SimpleSelect.subscribe()")
                if (init) {
                    init = false
                } else {
                    store.dispatch(FilterByTag(value))
                }
            }
        }
        div { +"Page: " }
    }

    /**
     * Gets all tags on all tasks as well as on all subtasks.
     */
    private fun getAllTags() : List<StringPair> {
        return store.state.tasks
            .flatMap { task ->
                task.tags.plus(
                    task.subtasks.flatMap { subtask ->
                        subtask.tags
                    }
                )
            }
            .distinct()
            .minus(store.state.settings.columnTags.map { it.tag }.toSet())
            .sorted()
            .map { tag -> tag to tag }
    }
}