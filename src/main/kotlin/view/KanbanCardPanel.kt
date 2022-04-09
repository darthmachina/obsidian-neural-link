package view

import io.kvision.core.*
import io.kvision.form.check.checkBox
import io.kvision.html.*
import io.kvision.panel.GridPanel
import io.kvision.panel.VPanel
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.datetime.*
import model.Note
import model.Task
import model.TaskConstants
import model.TaskModel
import org.reduxkotlin.Store
import service.RepeatingTaskService
import store.SubtaskCompleted
import store.TaskCompleted

class KanbanCardPanel(
    val store: Store<TaskModel>,
    val task: Task,
    private val status: String,
    private val repeatingTaskService: RepeatingTaskService
): VPanel(spacing = 5) {
    init {
        addCssStyle(KanbanStyles.KANBAN_CARD)
        // Description
        // Tags & Due
        val filteredTags = task.tags
            .filter { tag -> tag != status }
            .plus(task.subtasks.flatMap { subtask -> subtask.tags })
            .distinct()
        if (filteredTags.isNotEmpty() || task.dueOn != null) {
            hPanel {
                addCssStyle(KanbanStyles.KANBAN_TAGS_DUE_PANEL)
                if (task.dueOn != null) {
                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val todayDate = LocalDate(today.year, today.monthNumber, today.dayOfMonth)
                    div {
                        addCssStyle(KanbanStyles.KANBAN_DUE)
                        if (task.dueOn!! < todayDate) {
                            background = Background(color = Color.name(Col.DARKRED))
                        } else if (task.dueOn!! == todayDate) {
                            background = Background(color = Color.name(Col.DARKGREEN))
                        } else if (task.dueOn!!.until(todayDate, DateTimeUnit.DAY) == -1) {
                            background = Background(color = Color.name(Col.DARKBLUE))
                        }
                        +task.dueOn.toString()
                    }
                }

                if (filteredTags.isNotEmpty()) {
                    div {
                        addCssStyle(KanbanStyles.KANBAN_TAG_LIST)
                        filteredTags.forEach { tag ->
                            span {
                                addCssStyle(KanbanStyles.KANBAN_TAG)
                                +"#$tag"
                            }
                        }
                    }
                }
            }
        }

        div {
            addCssStyle(KanbanStyles.KANBAN_DESCRIPTION)
            checkBox(task.completed, label = task.description) {
                inline = true
            }.onClick {
                store.dispatch(TaskCompleted(task.id, repeatingTaskService))
            }
        }

        // Subtasks
        if (task.subtasks.isNotEmpty()) {
            vPanel {
                addCssStyle(KanbanStyles.KANBAN_SUBTASKS)
                task.subtasks.forEach { subtask ->
                    div {
                        checkBox(subtask.completed, label = subtask.description) {
                            inline = true
                        }.onClick {
                            store.dispatch(SubtaskCompleted(task.id, subtask.id, this.value))
                        }
                    }
                }
            }
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
        val filteredDataviewFields = task.dataviewFields.filter { entry -> entry.key != TaskConstants.TASK_ORDER_PROPERTY }
        if (filteredDataviewFields.isNotEmpty()) {
            table {
                addCssStyle(KanbanStyles.KANBAN_DATAVIEW_TABLE)
                filteredDataviewFields.forEach { entry ->
                    tr {
                        td {
                            addCssStyle(KanbanStyles.KANBAN_DATAVIEW_LABEL)
                            +entry.key
                        }
                        td {
                            addCssStyle(KanbanStyles.KANBAN_DATAVIEW_VALUE)
                            +entry.value
                        }
                    }
                }
            }
        }

        // Source
        div {
            addCssStyle(KanbanStyles.KANBAN_SOURCE)
            +task.file.dropLast(3)
        }
    }

    /**
     * Recursive function that will output an unordered list of notes with all subnotes in a hierarchy. Recursion stops
     * when the subnotes list is empty.
     */
    private fun outputNotes(notes: List<Note>) {
        ul {
            addCssStyle(KanbanStyles.KANBAN_NOTES)
            notes.forEach { note ->
                li {
                    +note.note

                    if (note.subnotes.isNotEmpty()) {
                        outputNotes(note.subnotes)
                    }
                }
            }
        }
    }
}
