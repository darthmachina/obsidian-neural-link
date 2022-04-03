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

        val KANBAN_CARD_LIST = Style(".nl-kanban-card-list") {
            width = 272.px
            minWidth = 272.px
            height = 100.perc
            overflowX = Overflow.HIDDEN
            overflowY = Overflow.AUTO
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

        val KANBAN_TAG = Style(".nl-kanban-tag") {
            border = Border(1.px, BorderStyle.SOLID, Color.name(Col.GRAY))
            borderRadius = 8.px
            paddingLeft = 8.px
            paddingRight = 8.px
            paddingTop = 2.px
            paddingBottom = 3.px
            textAlign = TextAlign.CENTER
            verticalAlign = VerticalAlign.TEXTBOTTOM
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
        }

        val KANBAN_DATAVIEW_TABLE = Style(".nl-kanban-dataview-table") {
            width = 100.perc
            fontSize = 13.px
        }

        val KANBAN_DATAVIEW_LABEL = Style(".nl-kanban-dataview-label") {
            width = 25.perc
        }

        val KANBAN_DATAVIEW_VALUE = Style(".nl-kanban-dataview-value") {
            borderLeft = Border(1.px, BorderStyle.DOTTED, Color.name(Col.GRAY))
            paddingLeft = 7.px
            wordBreak = WordBreak.BREAKALL
        }

        val KANBAN_DUE = Style(".nl-kanban-due") {
            border = Border(1.px, BorderStyle.SOLID, Color.name(Col.GRAY))
            borderRadiusList = listOf(0.px, 0.px, 0.px, 8.px)
            background = Background(color = Color.name(Col.DIMGRAY))
            paddingLeft = 4.px
            paddingRight = 5.px
            fontSize = 11.px
            position = Position.ABSOLUTE
            top = (-5).px
            right = (-5).px
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
