package view

import io.kvision.core.*
import io.kvision.utils.px

class KanbanStyles {
    companion object {
        val KANBAN_COLUMN = Style {
            width = 272.px
            margin = 15.px
        }

        val DRAG_OVER_CARD = Style {
            borderTop = Border(1.px, BorderStyle.DASHED, Color.name(Col.RED))
        }
    }
}