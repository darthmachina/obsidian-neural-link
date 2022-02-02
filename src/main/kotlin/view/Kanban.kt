@file:Suppress("NON_EXPORTABLE_TYPE")

package view

import Task
import react.Props
import react.RBuilder
import react.RComponent
import react.State
import styled.css
import styled.styledDiv
import styled.styledLi
import styled.styledUl

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
external interface KanbanProps : Props {
    var columns: List<String>
    var tasks: Map<String,List<Task>>
}

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
data class KanbanState(
    val columns: List<String>,
    val tasks: Map<String,List<Task>>
) : State

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class Kanban(props: KanbanProps) : RComponent<KanbanProps, KanbanState>(props) {
    init {
        state = KanbanState(props.columns, props.tasks)
    }

    // Board -> Column -> Card
    override fun RBuilder.render() {
        styledDiv {
            css {
                +KanbanStyles.kanbanBoard
            }

            state.columns.forEach { column ->
                styledUl {
                    css {
                        +KanbanStyles.kanbanColumn
                    }

                    state.tasks[column]?.forEach { task ->
                        styledLi {
                            css {
                                +KanbanStyles.kanbanCard
                            }
                            +task.description
                        }
                    }
                }
            }
        }
    }
}
