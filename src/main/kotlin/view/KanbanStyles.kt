package view

import io.kvision.core.*
import io.kvision.utils.perc
import io.kvision.utils.px

class KanbanStyles {
    companion object {
        val ROOT = Style(".nl-root") {
            height = 100.perc
            overflow = Overflow.SCROLL
        }

        val KANBAN_COLUMN = Style(".nl-kanban-column") {
            width = 272.px
            minWidth = 272.px
            margin = 15.px
            height = 100.perc
        }

        val DRAG_OVER_CARD = Style(".nl-kanban-card") {
            borderTop = Border(1.px, BorderStyle.DASHED, Color.name(Col.RED))
        }
    }
}
