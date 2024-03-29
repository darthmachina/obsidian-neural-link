package neurallink.view

import io.kvision.core.*
import io.kvision.utils.auto
import io.kvision.utils.perc
import io.kvision.utils.px

class KanbanStyles {
    companion object {
        val ROOT = Style(".nl-root") {
            height = 100.perc
            maxHeight = 100.perc
        }

        val TEXT_BOLD = Style(".nl-bold") {
            fontWeight = FontWeight.BOLDER
        }

        val TEXT_ITALIC = Style(".nl-italic") {
            fontStyle = FontStyle.ITALIC
        }

        val TEXT_LINK = Style(".nl-wikilink") {
            textDecoration = TextDecoration(TextDecorationLine.UNDERLINE)
            color = Color.name(Col.DEEPSKYBLUE)
        }

        val KANBAN_HEADER = Style(".nl-kanban-header") {
            width = 100.perc
            height = 25.px
            marginRight = 0.px
            marginLeft = auto
            marginTop = 4.px
            marginBottom = 8.px

            style("div") {
                style("div") {
                    style("label") {
                        position = Position.RELATIVE
                        top = (-2).px
                    }
                }
                style(".form-check") {
                    marginTop = 6.px
                }
            }
        }

        val KANBAN_HEADER_LABEL = Style(".nl-kanban-header-label") {
            marginTop = 5.px
        }

        val SELECT_INPUT = Style(".nl-select-input") {
            width = 150.px
            color = Color.name(Col.WHITE)
        }

        val KANBAN_COLUMNS = Style(".nl-kanban-columns") {
            height = 100.perc
            maxHeight = 100.perc
            width = 100.perc
            overflowX = Overflow.AUTO
            overflowY = Overflow.HIDDEN
            background = Background(color = Color.hex(0x343434))
        }

        val KANBAN_COLUMN = Style(".nl-kanban-column") {
            width = 272.px
            minWidth = 272.px
            margin = 10.px
            maxHeight = 95.perc
            background = Background(color = Color.hex(0x1e1e1e))
        }

        val KANBAN_COLUMN_NAME = Style(".nl-kanban-column-name") {
            marginBottom = 3.px
        }

        val KANBAN_CARD_LIST = Style(".nl-kanban-card-list") {
            width = 266.px
            minWidth = 266.px
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
            border = Border(1.px, BorderStyle.SOLID, Color.hex(0x5a5a5a))
            borderLeft = Border(5.px, BorderStyle.SOLID, Color.hex(0x5a5a5a))
        }

        val KANBAN_TAGS_DUE_PANEL = Style(".nl-kanban-tags-due-panel") {
            position = Position.RELATIVE
            marginLeft = (-5).px
            paddingLeft = 5.px
            marginRight = (-5).px
            paddingRight = 5.px
            paddingBottom = 5.px
            marginTop = (-5).px
            paddingTop = 5.px
            background = Background(color = Color.hex(0x181818))
        }

        val KANBAN_TAG_LIST = Style(".nl-kanban-tag-list") {
            fontSize = 75.perc
        }

        val KANBAN_TAG = Style(".nl-kanban-tag") {
            border = Border(1.px, BorderStyle.SOLID, Color.name(Col.GRAY))
            borderRadius = 8.px
            paddingTop = (-4).px
            paddingBottom = 0.px
            paddingLeft = 8.px
            paddingRight = 8.px
            textAlign = TextAlign.CENTER
            verticalAlign = VerticalAlign.TEXTBOTTOM
        }

        val KANBAN_DESCRIPTION = Style(".nl-kanban-description") {
            background = Background(color = Color.hex(0x181818))
            paddingTop = 7.px
            marginTop = (-5).px
            marginLeft = (-5).px
            paddingLeft = 5.px
            marginRight = (-5).px
            paddingRight = 5.px
            borderBottom = Border(1.px, BorderStyle.SOLID, Color.hex(0x5a5a5a))

            style("div") {
                style("div") {
                    style("label") {
                        position = Position.RELATIVE
                        top = (-2).px
                    }
                }
            }
        }

        val KANBAN_SUBTASKS = Style(".nl-kanban-subtask-list") {
            paddingLeft = 20.px
            fontSize = 75.perc

            style("div") {
                style("div") {
                    style("label") {
                        position = Position.RELATIVE
                        top = (-2).px
                    }
                }
            }
        }

        val KANBAN_NOTES = Style(".nl-kanban-notes-list") {
            marginLeft = 4.px
            marginTop = 0.px
            fontSize = 75.perc

            style("li") {
                marginBottom = 5.px
            }
        }

        val KANBAN_SUBNOTES = Style(".nl-kanban-subnotes-list") {
            marginLeft = (-20).px
            marginTop = 0.px
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
            top = (-1).px
            right = (-1).px
        }

        val KANBAN_SOURCE = Style(".nl-kanban-source") {
            fontSize = 11.px
            position = Position.ABSOLUTE
            right = (-3).px
            bottom = (-5).px
        }

        val KANBAN_BUTTONS = Style(".nl-kanban-buttons") {
            fontSize = 11.px
            position = Position.ABSOLUTE
            left = 1.px
            bottom = (-5).px
        }

        val KANBAN_BUTTON = Style(".nl-kanban-button") {
            padding = 1.px
            marginRight = 4.px
            marginBottom = 6.px
            height = 20.px
        }

        val DRAG_OVER_CARD = Style(".nl-kanban-card-dragover") {
            borderTop = Border(1.px, BorderStyle.DASHED, Color.name(Col.RED))
        }
    }
}
