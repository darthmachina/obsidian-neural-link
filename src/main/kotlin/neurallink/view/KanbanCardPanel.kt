package neurallink.view

import MarkdownView
import WorkspaceLeaf
import io.kvision.core.*
import io.kvision.form.check.CheckBox
import io.kvision.form.check.checkBox
import io.kvision.html.*
import io.kvision.panel.HPanel
import io.kvision.panel.VPanel
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.utils.px
import kotlinx.datetime.*
import mu.KotlinLogging
import neurallink.core.model.*
import neurallink.core.model.Tag
import neurallink.core.store.IncompleteSubtaskChoice
import neurallink.core.store.MoveToTop
import neurallink.core.store.SubtaskCompleted
import neurallink.core.store.TaskCompleted
import neurallink.core.store.TaskMoved
import org.reduxkotlin.Store
import org.w3c.dom.events.MouseEvent

private val logger = KotlinLogging.logger("KanbanCardPanel")

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
            add(createCheckbox(task) {
                if (task.subtasks.any { !it.completed }) {
                    // Incomplete subtasks exist, ask what to do. Dialog completes the task is requested.
                    askAboutIncompleteSubtasks(task)
                } else {
                    store.dispatch(TaskCompleted(task.id))
                }
            })
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
                    li(rich = true) {
                        add(createNotePanel(note.note, leaf))

                        if (note.subnotes.isNotEmpty()) {
                            ul {
                                addCssStyle(KanbanStyles.KANBAN_SUBNOTES)
                                note.subnotes.forEach { subnote1 ->
                                    li(rich = true) {
                                        add(createNotePanel(subnote1.note, leaf))

                                        if (subnote1.subnotes.isNotEmpty()) {
                                            ul {
                                                addCssStyle(KanbanStyles.KANBAN_SUBNOTES)
                                                subnote1.subnotes.forEach { subnote2 ->
                                                    li(rich = true) {
                                                        add(createNotePanel(subnote2.note, leaf))
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
        add(createFooterPanel(leaf, status))
    }

    private fun createNotePanel(text: String, leaf: WorkspaceLeaf) : Span {
        return span {
            rich = true
            parseMarkdownLinks(text).forEach {
                if (it.startsWith("!")) {
                    val link = it.drop(1)
                    link(link).onClick {
                        openSourceFile(link, leaf)
                    }
                } else {
                    +markdownToStyle(it)
                }
            }
        }
    }

    private fun createCheckbox(task: Task, handler: CheckBox.(MouseEvent) -> Unit) : HPanel {
        return hPanel {
            checkBox(
                task.completed,
                label = markdownToStyle(task.description.value),
                rich = true
            ) {
                inline = true
            }.onClick(handler = handler)
        }
    }

    private fun createTagsAndDuePanel(filteredTags: List<Tag>) : HPanel {
        return hPanel {
            addCssStyle(KanbanStyles.KANBAN_TAGS_DUE_PANEL)
            if (task.dueOn != null) {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val todayDate = LocalDate(today.year, today.monthNumber, today.dayOfMonth)
                div {
                    addCssStyle(KanbanStyles.KANBAN_DUE)
                    if (task.dueOn < todayDate) {
                        background = Background(color = Color.name(Col.DARKRED))
                    } else if (task.dueOn.value == todayDate) {
                        background = Background(color = Color.name(Col.DARKGREEN))
                    } else if (task.dueOn.value.until(todayDate, DateTimeUnit.DAY) == -1) {
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
            marginBottom = 0.px
            task.subtasks.forEach { subtask ->
                add(createCheckbox(subtask) {
                    store.dispatch(SubtaskCompleted(task.id, subtask.id, this.value))
                })
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

    private fun createFooterPanel(leaf: WorkspaceLeaf, statusTag: StatusTag) : HPanel {
        return hPanel {
            div {
                addCssStyle(KanbanStyles.KANBAN_BUTTONS)
                button("", icon = "fas fa-arrows-left-right") {
                    addCssStyle(KanbanStyles.KANBAN_BUTTON)
                    size = ButtonSize.SMALL
                    cursor = Cursor.POINTER
                }.onClick {
                    chooseNewStatus()
                }
                if (!statusTag.dateSort) {
                    button("", icon = "fas fa-angles-up") {
                        addCssStyle(KanbanStyles.KANBAN_BUTTON)
                        size = ButtonSize.SMALL
                        cursor = Cursor.POINTER
                    }.onClick {
                        store.dispatch(MoveToTop(task.id))
                    }
                }
            }
            div {
                addCssStyle(KanbanStyles.KANBAN_SOURCE)
                button(text = task.file.value.dropLast(3).split("/").last()) {
                    addCssStyle(KanbanStyles.KANBAN_BUTTON)
                    size = ButtonSize.SMALL
                    cursor = Cursor.POINTER
                }.onClick {
                    logger.debug { "going to source file" }
                    openSourceFile(task, leaf)
                }
            }
        }
    }

    private fun chooseNewStatus() {
        logger.debug { "chooseNewStatus()" }
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
            logger.debug { "Dialog callback(): $result" }
            if (result.accept && (result.result) != status.tag.value) {
                logger.debug { " - saving result: ${result.result}" }
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
        logger.debug { "askAboutIncompleteSubtasks()" }
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

    // TODO: Refactor both openSourceFile methods to redce duplicate code
    private fun openSourceFile(path: String, leaf: WorkspaceLeaf) {
        logger.debug { "openSourceFile()" }
        val filePath = leaf.view.app.metadataCache.getFirstLinkpathDest(path, "")
        if (filePath == null) {
            logger.error { " - ERROR: file path not found: $path" }
            return
        }

        val leavesWithFileAlreadyOpen = leaf.view.app.workspace.getLeavesOfType("markdown")
            .filter { leafOfType ->
                (leafOfType.view as MarkdownView).file.path == filePath.path
            }
        logger.debug { "Open leaf list : $leavesWithFileAlreadyOpen" }

        if (leavesWithFileAlreadyOpen.isNotEmpty()) {
            leaf.view.app.workspace.setActiveLeaf(leavesWithFileAlreadyOpen[0])
        } else {
            leaf.view.app.workspace.splitActiveLeaf().openFile(filePath)
        }
    }

    private fun openSourceFile(task: Task, leaf: WorkspaceLeaf) {
        logger.debug { "openSourceFile()" }
        val filePath = leaf.view.app.metadataCache.getFirstLinkpathDest(task.file.value, "")
        if (filePath == null) {
            logger.error { " - ERROR: file path not found: ${task.file}" }
            return
        }

        val leavesWithFileAlreadyOpen = leaf.view.app.workspace.getLeavesOfType("markdown")
            .filter { leafOfType ->
                (leafOfType.view as MarkdownView).file.path == task.file.value
            }
        logger.debug { "Open leaf list : $leavesWithFileAlreadyOpen" }

        val line = js("({})")
        line["line"] = task.filePosition.value

        if (leavesWithFileAlreadyOpen.isNotEmpty()) {
            leaf.view.app.workspace.setActiveLeaf(leavesWithFileAlreadyOpen[0])
            leavesWithFileAlreadyOpen[0].setEphemeralState(line)
        } else {
            val splitLeaf = leaf.view.app.workspace.splitActiveLeaf()
            splitLeaf.openFile(filePath).then {
                splitLeaf.setEphemeralState(line)
            }
        }
    }
}
