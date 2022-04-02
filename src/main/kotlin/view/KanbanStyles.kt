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
            height = 95.perc
        }

        val KANBAN_CARD = Style(".nl-kanban-card") {
            position = Position.RELATIVE
            width = 100.perc
            paddingTop = 5.px
            paddingBottom = 15.px
            paddingLeft = 5.px
            paddingRight = 5.px
            border = Border(1.px, BorderStyle.SOLID, Color.name(Col.WHITE))
        }

        val KANBAN_TAGS_DUE_PANEL = Style(".nl-kanban-tags-due-panel") {
            position = Position.RELATIVE
            paddingLeft = 5.px
        }

        val KANBAN_TAG_LIST = Style(".nl-kanban-tag-list") {
            fontSize = 13.px
        }

        val KANBAN_DESCRIPTION = Style(".nl-kanban-description") {
        }

        val KANBAN_SUBTASKS = Style(".nl-kanban-subtask-list") {
            paddingLeft = 20.px
        }

        val KANBAN_NOTES = Style(".nl-kanban-notes-list") {
            marginLeft = 8.px
            marginTop = 0.px
            fontSize = 15.px

//            style("li::marker") {
//                marginRight = 5.px
//            }
        }

        val KANBAN_DATAVIEW_TABLE = Style(".nl-kanban-dataview-table") {
            width = 100.perc
            fontSize = 13.px
        }

        val KANBAN_DUE = Style(".nl-kanban-due") {
            fontSize = 11.px
            position = Position.ABSOLUTE
            top = (-5).px
            right = 0.px
        }

        val KANBAN_SOURCE = Style(".nl-kanban-source") {
            fontSize = 11.px
            position = Position.ABSOLUTE
            right = 3.px
            bottom = (-5).px
        }

        val DRAG_OVER_CARD = Style(".nl-kanban-card-dragover") {
            borderTop = Border(1.px, BorderStyle.DASHED, Color.name(Col.RED))
        }
    }
}
