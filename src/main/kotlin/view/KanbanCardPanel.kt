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
import model.Task
import model.TaskModel
import org.reduxkotlin.Store
import store.SubtaskCompleted
import store.TaskCompleted

class KanbanCardPanel(val store: Store<TaskModel>, val task: Task): VPanel(spacing = 5) {
    init {
        addCssStyle(KanbanStyles.KANBAN_CARD)
        // Description
        // Tags & Due
        if (task.tags.isNotEmpty() || task.dueOn != null) {
            hPanel {
                addCssStyle(KanbanStyles.KANBAN_TAGS_DUE_PANEL)
                if (task.dueOn != null) {
                    div {
                        addCssStyle(KanbanStyles.KANBAN_DUE)
                        +task.dueOn.toString()
                    }
                }

                if (task.tags.isNotEmpty()) {
                    div {
                        addCssStyle(KanbanStyles.KANBAN_TAG_LIST)
                        task.tags.forEach { tag ->
                            span { +"#$tag" }
                        }
                    }
                }
            }
        }

        div {
            checkBox(task.completed, label = task.description) {
                inline = true
            }.onClick {
                store.dispatch(TaskCompleted(task.id))
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
            vPanel {
                addCssStyle(KanbanStyles.KANBAN_NOTES)
                task.notes.forEach { note ->
                    li {
                        +note
                    }
                }
            }
        }

        // Dataview Fields
        if (task.dataviewFields.isNotEmpty()) {
            table {
                addCssStyle(KanbanStyles.KANBAN_DATAVIEW_TABLE)
                task.dataviewFields.forEach { entry ->
                    tr {
                        td {
                            width = 25.perc
                            borderRight = Border(1.px, BorderStyle.DOTTED, Color.name(Col.GRAY))
                            +entry.key
                        }
                        td {
                            wordBreak = WordBreak.BREAKALL
                            +entry.value
                        }
                    }
                }
            }
        }

        // Source
        div {
            addCssStyle(KanbanStyles.KANBAN_SOURCE)
            +task.file.replace(".md", "")
        }
    }
}
