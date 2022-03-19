package view

import io.kvision.form.check.checkBox
import io.kvision.html.div
import io.kvision.html.li
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.panel.GridPanel
import model.Task
import model.TaskModel
import org.reduxkotlin.Store
import store.SubtaskCompleted
import store.TaskCompleted

class KanbanCard(val store: Store<TaskModel>, val task: Task): GridPanel(columnGap = 5, rowGap = 5) {
    init {
        addCssStyle(KanbanStyles.KANBAN_CARD)
        // Description
        div {
            checkBox(task.completed, label = task.description) {
                inline = true
            }.onClick {
                store.dispatch(TaskCompleted(task.id))
            }
        }

        // Tags
        div {
            task.tags.forEach { tag ->
                span { +"#$tag" }
            }
        }

        // Subtasks
        if (task.subtasks.isNotEmpty()) {
            div {
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
                task.notes.forEach { note ->
                    li {
                        +note
                    }
                }
            }
        }

        // Dataview Fields
        if (task.dataviewFields.isNotEmpty()) {
            ul {
                task.dataviewFields.forEach { entry ->
                    li {
                        +"${entry.key} : ${entry.value}"
                    }
                }
            }
        }

        // Due
        if (task.dueOn != null) {
            div {
                +"Due: ${task.dueOn}"
            }
        }

        // Source
        div {
            +task.file
        }
    }
}
