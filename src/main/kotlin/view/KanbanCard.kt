package view

import io.kvision.form.check.checkBox
import io.kvision.html.div
import io.kvision.html.li
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.panel.GridPanel
import model.Task

class KanbanCard(val task: Task): GridPanel(columnGap = 5, rowGap = 5) {
    init {
        addCssStyle(KanbanStyles.KANBAN_CARD)
        // Description
        div {
            checkBox(false, label = task.description) { inline = true }
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
                        checkBox(false, label = subtask.description) { inline = true }
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
