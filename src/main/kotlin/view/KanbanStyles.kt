package view

import kotlinx.css.Display
import kotlinx.css.GridAutoColumns
import kotlinx.css.GridAutoFlow
import kotlinx.css.LinearDimension
import kotlinx.css.display
import kotlinx.css.gridAutoColumns
import kotlinx.css.gridAutoFlow
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

    }

    val kanbanCard by css {

    }
}
