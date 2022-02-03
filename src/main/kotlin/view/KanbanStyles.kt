package view

import kotlinx.css.*
import kotlinx.css.properties.border
import kotlinx.css.properties.borderBottom
import styled.StyleSheet

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED", "NON_EXPORTABLE_TYPE")
@OptIn(ExperimentalJsExport::class)
@JsExport
object KanbanStyles : StyleSheet("KanbanStyles", isStatic = true) {
    val kanbanBoard by css {
        display = Display.grid
        gridAutoFlow = GridAutoFlow.column
        gridAutoColumns = GridAutoColumns.Companion.minMax(
            GridAutoColumns(LinearDimension("272px")),
            GridAutoColumns(LinearDimension("1fr")))
    }

    val kanbanColumn by css {
        display = Display.grid
        gridAutoFlow = GridAutoFlow.row
        margin(LinearDimension("1"))
        border(LinearDimension("1"), BorderStyle.solid, Color.white)
    }

    val columnHeader by css {
        margin(LinearDimension("1"))
        borderBottom(LinearDimension("1"), BorderStyle.solid, Color.white)
    }

    val kanbanCard by css {
        display = Display.grid
        gridAutoFlow = GridAutoFlow.row
        margin(LinearDimension("1"))
        border(LinearDimension("1"), BorderStyle.solid, Color.white)
    }
}
