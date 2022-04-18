package view

import io.kvision.core.*
import io.kvision.form.check.checkBox
import io.kvision.form.check.checkBoxInput
import io.kvision.form.select.SimpleSelect
import io.kvision.form.select.SimpleSelectInput
import io.kvision.form.select.simpleSelect
import io.kvision.form.select.simpleSelectInput
import io.kvision.html.ButtonSize
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.panel.HPanel
import io.kvision.utils.perc
import io.kvision.utils.px
import model.TaskConstants
import model.TaskModel
import org.reduxkotlin.Store
import store.FilterByDataviewValue
import store.FilterByFile
import store.FilterByTag
import store.FilterFutureDate

class KanbanHeader(val store: Store<TaskModel>) : HPanel(spacing = 10, justify = JustifyContent.END) {
    private var filtering = false
    private val tagSelect: SimpleSelectInput = SimpleSelectInput()
    private val fileSelect: SimpleSelectInput = SimpleSelectInput()
    private val dataviewSelect: SimpleSelectInput = SimpleSelectInput()

    init {
        addCssStyle(KanbanStyles.KANBAN_HEADER)
        button("Clear") {
            size = ButtonSize.SMALL
            padding = 3.px
            margin = 1.px
            marginRight = 5.px
        }.onClick {
            console.log("Clear.onClick()")
        }
        button("Filter") {
            size = ButtonSize.SMALL
            padding = 3.px
            margin = 1.px
            marginRight = 5.px
        }.onClick {
            console.log("Filter.onClick()")
        }
//        div { +"Filters" }
//        div {
//            checkBox(label = "!Future") {
//                inline = true
//                minWidth = 60.px
//            }.onClick {
//                store.dispatch(FilterFutureDate(this.value))
//            }
//        }
//        div { +"Tag: " }
//        tagSelect = simpleSelectInput(getAllTags(), emptyOption = true) {
//            addCssStyle(KanbanStyles.SELECT_INPUT)
//            style("select > option") {
//                background = Background(color = Color.name(Col.BLACK))
//            }
//
//            var init = true
//            subscribe {
//                console.log("tagSelect.subscribe()", it)
//                if (init) {
//                    init = false
//                } else {
//                    filterByTag(it)
//                }
//            }
//        }
//        div { +"Page: " }
//        fileSelect = simpleSelectInput(getAllFiles(), emptyOption = true) {
//            addCssStyle(KanbanStyles.SELECT_INPUT)
//            style("select > option") {
//                background = Background(color = Color.name(Col.BLACK))
//            }
//
//            var init = true
//            subscribe {
//                console.log("fileSelect.subscribe()", it)
//                if (init) {
//                    init = false
//                } else {
//                    filterByFile(it)
//                }
//            }
//        }
//        div { +"Dataview: " }
//        dataviewSelect = simpleSelectInput(getAllDataviewFields(), emptyOption = true) {
//            addCssStyle(KanbanStyles.SELECT_INPUT)
//            style("select > option") {
//                background = Background(color = Color.name(Col.BLACK))
//            }
//
//            var init = true
//            subscribe {
//                console.log("fileSelect.subscribe()", it)
//                if (init) {
//                    init = false
//                } else {
//                    filterByDataviewValue(it)
//                }
//            }
//        }
    }

    fun filterByTag(tag: String?) {
        if (!filtering) {
            filtering = true
            fileSelect.value = null
            dataviewSelect.value = null
            store.dispatch(FilterByTag(tag))
            filtering = false
        }
    }

    fun filterByFile(file: String?) {
        if (!filtering) {
            filtering = true
            tagSelect.value = null
            dataviewSelect.value = null
            store.dispatch(FilterByFile(file))
            filtering = false
        }
    }

    fun filterByDataviewValue(value: String?) {
        if (!filtering) {
            filtering = true
            tagSelect.value = null
            fileSelect.value = null
            store.dispatch(FilterByDataviewValue(value))
            filtering = false
        }
    }

    /**
     * Gets all tags on all tasks as well as on all subtasks.
     */
    fun getAllTags() : List<StringPair> {
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

    /**
     * Gets all source files on all tasks.
     */
    fun getAllFiles() : List<StringPair> {
        return store.state.tasks
            .map { task ->
                task.file
            }
            .distinct()
            .sorted()
            .map { it to it.dropLast(3) }
    }

    /**
     * Returns a simplistic list of all dataview field/value pairs; ignoring the TASK_ORDER_PROPERTY.
     */
    fun getAllDataviewFields() : List<StringPair> {
        return store.state.tasks
            .flatMap { task ->
                task.dataviewFields.entries
            }
            .asSequence()
            .filter { it.key != TaskConstants.TASK_ORDER_PROPERTY }
            .map { entry ->
                "${entry.key}::${entry.value}"
            }
            .distinct()
            .sorted()
            .map { it to it }
            .toList()
    }
}