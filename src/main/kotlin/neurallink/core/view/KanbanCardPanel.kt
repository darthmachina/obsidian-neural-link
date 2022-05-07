package neurallink.core.view

import MarkdownView
import WorkspaceLeaf
import io.kvision.core.*
import io.kvision.form.check.checkBox
import io.kvision.html.ButtonSize
import io.kvision.html.Table
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.li
import io.kvision.html.span
import io.kvision.html.table
import io.kvision.html.td
import io.kvision.html.tr
import io.kvision.html.ul
import io.kvision.panel.HPanel
import io.kvision.panel.VPanel
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.utils.px
import kotlinx.datetime.*
import neurallink.core.model.*
import neurallink.core.store.IncompleteSubtaskChoice
import neurallink.core.store.MoveToTop
import neurallink.core.store.SubtaskCompleted
import neurallink.core.store.TaskCompleted
import neurallink.core.store.TaskMoved
import org.reduxkotlin.Store

class KanbanCardPanel(
    leaf: WorkspaceLeaf,
    val store: Store<NeuralLinkModel>,
    val task: Task,
    private val status: StatusTag
): VPanel(spacing = 5) {
    init {
        addCssStyle(KanbanStyles.KANBAN_CARD)
        // Description
        // Tags & Due
        val filteredTags = task.tags
            .filter { tag -> tag != status.tag }
            .plus(task.subtasks.flatMap { subtask -> subtask.tags })
            .distinct()
        if (filteredTags.isNotEmpty() || task.dueOn != null) {
            add(createTagsAndDuePanel(filteredTags))
        }

        div {
            addCssStyle(KanbanStyles.KANBAN_DESCRIPTION)
            checkBox(task.completed, label = task.description.value) {
                inline = true
            }.onClick {
                if (task.subtasks.any { !it.completed }) {
                    // Incomplete subtasks exist, ask what to do. Dialog completes the task is requested.
                    askAboutIncompleteSubtasks(task)
                } else {
                    store.dispatch(TaskCompleted(task.id))
                }
            }
        }

        // Subtasks
        if (task.subtasks.isNotEmpty()) {
            add(createSubtaskPanel())
        }

        // Notes
        if (task.notes.isNotEmpty()) {
            ul {
                addCssStyle(KanbanStyles.KANBAN_NOTES)
                task.notes.forEach { note ->
                    li {
                        +note.note

                        if (note.subnotes.isNotEmpty()) {
                            ul {
                                addCssStyle(KanbanStyles.KANBAN_SUBNOTES)
                                note.subnotes.forEach { subnote1 ->
                                    li {
                                        +subnote1.note

                                        if (subnote1.subnotes.isNotEmpty()) {
                                            ul {
                                                addCssStyle(KanbanStyles.KANBAN_SUBNOTES)
                                                subnote1.subnotes.forEach { subnote2 ->
                                                    li {
                                                        +subnote2.note
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dataview Fields
        val filteredDataviewFields = task.dataviewFields.filter { entry -> entry.key != DataviewField(TaskConstants.TASK_ORDER_PROPERTY) }.toDataviewMap()
        if (filteredDataviewFields.isNotEmpty()) {
            add(createDataviewPanel(filteredDataviewFields))
        }

        // Source & Buttons
        add(createFooterPanel(leaf))
    }

    private fun createTagsAndDuePanel(filteredTags: List<Tag>) : HPanel {
        return hPanel {
            addCssStyle(KanbanStyles.KANBAN_TAGS_DUE_PANEL)
            if (task.dueOn != null) {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val todayDate = LocalDate(today.year, today.monthNumber, today.dayOfMonth)
                div {
                    addCssStyle(KanbanStyles.KANBAN_DUE)
                    if (task.dueOn!! < todayDate) {
                        background = Background(color = Color.name(Col.DARKRED))
                    } else if (task.dueOn!!.value == todayDate) {
                        background = Background(color = Color.name(Col.DARKGREEN))
                    } else if (task.dueOn!!.value.until(todayDate, DateTimeUnit.DAY) == -1) {
                        background = Background(color = Color.name(Col.DARKBLUE))
                    }
                    +task.dueOn.toString()
                }
            }

            if (filteredTags.isNotEmpty()) {
                hPanel(spacing = 5) {
                    flexWrap = FlexWrap.WRAP
                    addCssStyle(KanbanStyles.KANBAN_TAG_LIST)
                    filteredTags.forEach { tag ->
                        span {
                            addCssStyle(KanbanStyles.KANBAN_TAG)
                            if (tag in store.state.settings.tagColors.keys) {
                                background = Background(color = Color("#" + store.state.settings.tagColors[tag]!!))
                            }
                            +"#$tag"
                        }
                    }
                }
            }
        }
    }
    private fun createSubtaskPanel() : VPanel {
        return vPanel {
            addCssStyle(KanbanStyles.KANBAN_SUBTASKS)
            task.subtasks.forEach { subtask ->
                div {
                    checkBox(subtask.completed, label = subtask.description.value) {
                        inline = true
                    }.onClick {
                        store.dispatch(SubtaskCompleted(task.id, subtask.id, this.value))
                    }
                }
            }
        }
    }

    private fun createDataviewPanel(filteredDataviewFields: DataviewMap) : Table {
        return table {
            addCssStyle(KanbanStyles.KANBAN_DATAVIEW_TABLE)
            filteredDataviewFields.forEach { entry ->
                tr {
                    td {
                        addCssStyle(KanbanStyles.KANBAN_DATAVIEW_LABEL)
                        +entry.key.value
                    }
                    td {
                        addCssStyle(KanbanStyles.KANBAN_DATAVIEW_VALUE)
                        +entry.value.value.toString()
                    }
                }
            }
        }
    }

    private fun createFooterPanel(leaf: WorkspaceLeaf) : HPanel {
        return hPanel {
            div {
                addCssStyle(KanbanStyles.KANBAN_BUTTONS)
                button("", icon = "fas fa-arrows-left-right") {
                    addCssStyle(KanbanStyles.KANBAN_BUTTON)
                    size = ButtonSize.SMALL
                }.onClick {
                    chooseNewStatus()
                }
                button("", icon = "fas fa-angles-up") {
                    addCssStyle(KanbanStyles.KANBAN_BUTTON)
                    size = ButtonSize.SMALL
                }.onClick {
                    store.dispatch(MoveToTop(task.id))
                }
            }
            div {
                addCssStyle(KanbanStyles.KANBAN_SOURCE)
                button(text = task.file.value.dropLast(3)) {
                    size = ButtonSize.SMALL
                    padding = 1.px
                    paddingBottom = 0.px
                    marginBottom = 4.px
                    marginRight = (-3).px
                }.onClick {
                    console.log("going to source file")
                    openSourceFile(task, leaf)
                }
            }
        }
    }

    private fun chooseNewStatus() {
        console.log("chooseNewStatus()")
        val statusSelect = DialogInput.SELECT.apply {
            model = store.state.settings.columnTags.map { it.tag.value to it.displayName }
            current = status.tag.value
        }
        val dialog = Dialog(
            "Change Status",
            "Choose the new status for this card",
            input = statusSelect
        )
        dialog.setCallback { result ->
            console.log("Dialog callback()", result)
            if (result.accept && (result.result) != status.tag.value) {
                console.log(" - saving result: ", result.result)
                val statusTag = store.state.settings.columnTags.find { it.tag.value == result.result }!!
                store.dispatch(TaskMoved(task.id, statusTag))
            }
            dialog.hide()
            remove(dialog)
        }
        add(dialog)
        dialog.show(true)
    }

    private fun askAboutIncompleteSubtasks(task: Task) {
        console.log("askAboutIncompleteSubtasks()")
        val subtaskChoices = DialogInput.SELECT.apply {
            model = listOf(
                "nothing" to "Do nothing",
                "complete" to "Complete them all",
                "delete" to "Delete incomplete subtasks"
            )
            current = "nothing"
        }
        val dialog = Dialog(
            "Incomplete Subtasks",
            "What should be done with the incomplete subtasks?",
            input = subtaskChoices
        )
        dialog.setCallback { result ->
//            store.dispatch(TaskCompleted(task.id, repeatingTaskService))
            if (result.accept) {
                val subtaskChoice = when(result.result) {
                    "nothing" -> IncompleteSubtaskChoice.NOTHING
                    "complete" -> IncompleteSubtaskChoice.COMPLETE
                    "delete" -> IncompleteSubtaskChoice.DELETE
                    else -> throw IllegalStateException("Subtask Choice result is not handled: ${result.result}")
                }
                store.dispatch(TaskCompleted(task.id, subtaskChoice))
            }
            dialog.hide()
            remove(dialog)
        }
        add(dialog)
        dialog.show(true)
    }

    private fun openSourceFile(task: Task, leaf: WorkspaceLeaf) {
        console.log("openSourceFile()")
        val filePath = leaf.view.app.metadataCache.getFirstLinkpathDest(task.file.value, "")
        if (filePath == null) {
            console.log(" - ERROR: file path not found: ${task.file}")
            return
        }

        val leavesWithFileAlreadyOpen = mutableListOf<WorkspaceLeaf>()
        leaf.view.app.workspace.iterateAllLeaves { workspaceLeaf ->
            if (workspaceLeaf.view is MarkdownView && (workspaceLeaf.view as MarkdownView).file.path == task.file.value) {
                leavesWithFileAlreadyOpen.add(workspaceLeaf)
            }
        }

        if (leavesWithFileAlreadyOpen.isNotEmpty()) {
            leaf.view.app.workspace.setActiveLeaf(leavesWithFileAlreadyOpen[0])
            leavesWithFileAlreadyOpen[0].setEphemeralState(object { val line = task.filePosition.value - 1 })
        } else {
            val splitLeaf = leaf.view.app.workspace.splitActiveLeaf()
            splitLeaf.openFile(filePath)
            splitLeaf.setEphemeralState(object { val line = task.filePosition.value - 1 })
        }
    }
}
