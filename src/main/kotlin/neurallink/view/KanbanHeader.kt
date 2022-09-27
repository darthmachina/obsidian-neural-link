package neurallink.view

import App
import io.kvision.core.*
import io.kvision.form.check.checkBox
import io.kvision.form.select.SimpleSelectInput
import io.kvision.form.select.simpleSelectInput
import io.kvision.html.ButtonSize
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.panel.HPanel
import io.kvision.utils.px
import mu.KotlinLogging
import neurallink.core.model.TaskConstants
import neurallink.core.model.NeuralLinkModel
import neurallink.core.model.DataviewField
import neurallink.core.service.loadTasKModelIntoStore
import neurallink.core.store.FilterByDataviewValue
import neurallink.core.store.FilterByFile
import neurallink.core.store.FilterByTag
import neurallink.core.store.FilterFutureDate
import neurallink.core.store.MoveToTop
import org.reduxkotlin.Store

private val logger = KotlinLogging.logger("NeuralLinkPlugin")

class KanbanHeader(val store: Store<NeuralLinkModel>, val app: App) : HPanel(spacing = 10, justify = JustifyContent.END) {
    private var filtering = false
    private val tagSelect: SimpleSelectInput
    private val fileSelect: SimpleSelectInput
    private val dataviewSelect: SimpleSelectInput

    init {
        addCssStyle(KanbanStyles.KANBAN_HEADER)
        div {
            addCssStyle(KanbanStyles.KANBAN_HEADER_LABEL)
            +"Filters"
        }
        div {
            addCssStyle(KanbanStyles.KANBAN_HEADER_LABEL)
            +"Dataview: "
        }
        dataviewSelect = simpleSelectInput(getAllDataviewFields(), emptyOption = true) {
            addCssStyle(KanbanStyles.SELECT_INPUT)
            style("select > option") {
                background = Background(color = Color.name(Col.BLACK))
            }

            var init = true
            subscribe {
                logger.debug { "dataviewSelect.subscribe(): $it" }
                if (init) {
                    init = false
                } else {
                    filterByDataviewValue(it)
                }
            }
        }
        div {
            addCssStyle(KanbanStyles.KANBAN_HEADER_LABEL)
            +"Tag: "
        }
        tagSelect = simpleSelectInput(getAllTags(), emptyOption = true) {
            addCssStyle(KanbanStyles.SELECT_INPUT)
            style("select > option") {
                background = Background(color = Color.name(Col.BLACK))
            }

            var init = true
            subscribe {
                logger.debug { "tagSelect.subscribe(): $it" }
                if (init) {
                    init = false
                } else {
                    filterByTag(it)
                }
            }
        }
        div {
            checkBox(label = "!Future") {
                inline = true
                minWidth = 84.px
            }.onClick {
                store.dispatch(FilterFutureDate(this.value))
            }
        }
        div {
            addCssStyle(KanbanStyles.KANBAN_HEADER_LABEL)
            +"Page: "
        }
        fileSelect = simpleSelectInput(createSourcefileSelectList(store.state.sourceFiles), emptyOption = true) {
            addCssStyle(KanbanStyles.SELECT_INPUT)
            style("select > option") {
                background = Background(color = Color.name(Col.BLACK))
            }

            var init = true
            subscribe {
                logger.debug { "fileSelect.subscribe(): $it" }
                if (init) {
                    init = false
                } else {
                    filterByFile(it)
                }
            }
        }
        div {
            button("", icon = "fas fa-arrows-rotate") {
                addCssStyle(KanbanStyles.KANBAN_BUTTON)
                size = ButtonSize.SMALL
                cursor = Cursor.POINTER
            }.onClick {
                loadTasKModelIntoStore(
                    app.vault,
                    app.metadataCache,
                    store
                )
            }
        }
        store.subscribe {
            fileSelect.options = createSourcefileSelectList(store.state.sourceFiles)
        }
    }

    private fun filterByTag(tag: String?) {
        if (!filtering) {
            filtering = true
            fileSelect.value = null
            dataviewSelect.value = null
            store.dispatch(FilterByTag(tag))
            filtering = false
        }
    }

    private fun filterByFile(file: String?) {
        if (!filtering) {
            filtering = true
            tagSelect.value = null
            dataviewSelect.value = null
            store.dispatch(FilterByFile(file))
            filtering = false
        }
    }

    private fun filterByDataviewValue(value: String?) {
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
    private fun getAllTags() : List<StringPair> {
        return store.state.tasks
            .flatMap { task ->
                task.tags.plus(
                    task.subtasks.flatMap { subtask ->
                        subtask.tags
                    }
                )
            }
            .asSequence()
            .map {
                it.value
            }
            .distinct()
            .minus(store.state.settings.columnTags.map { it.tag.value }.toSet())
            .sorted()
            .map { tag -> tag to tag }
            .toList()
    }

    /**
     * Returns a simplistic list of all dataview field/value pairs; ignoring the TASK_ORDER_PROPERTY.
     */
    private fun getAllDataviewFields() : List<StringPair> {
        return store.state.tasks
            .flatMap { task ->
                task.dataviewFields.entries
            }
            .asSequence()
            .filter { it.key != DataviewField(TaskConstants.TASK_ORDER_PROPERTY) }
            .map { entry ->
                "${entry.key}::${entry.value}"
            }
            .distinct()
            .sorted()
            .map { it to it }
            .toList()
    }

    private fun createSourcefileSelectList(files: List<String>) : List<StringPair> {
        return files.map { file ->
            "$file.md" to file
        }
    }
}
