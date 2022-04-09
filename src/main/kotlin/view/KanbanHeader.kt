package view

import io.kvision.core.AlignItems
import io.kvision.core.JustifyContent
import io.kvision.html.div
import io.kvision.panel.HPanel
import model.TaskModel
import org.reduxkotlin.Store

class KanbanHeader(val store: Store<TaskModel>) : HPanel(spacing = 5, justify = JustifyContent.END) {
    init {
        addCssStyle(KanbanStyles.KANBAN_HEADER)
        div { +"Tag Filter" }
        div { +"Page Filter" }
    }
}