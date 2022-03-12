package view

import io.kvision.html.div
import io.kvision.panel.HPanel
import io.kvision.panel.VPanel
import io.kvision.utils.px
import model.TaskModel
import org.reduxkotlin.Store

class KanbanBoard(val store: Store<TaskModel>): HPanel() {
    init {
        // TODO add a listener to look for changes in the columns
        store.state.kanbanColumns.forEach { (name, cards) ->
            add(createColumn(name))
        }
    }

    private fun createColumn(name: String): VPanel {
        val column = VPanel(spacing = 5) {
            width = 272.px
            div {
                +name
            }
        }

        return column
    }
}