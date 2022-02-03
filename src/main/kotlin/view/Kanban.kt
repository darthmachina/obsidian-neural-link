@file:Suppress("NON_EXPORTABLE_TYPE")

package view

import Task
import react.Props
import react.RBuilder
import react.RComponent
import react.State
import react.dom.hr
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
        console.log("render()")
        styledDiv {
            css {
                +KanbanStyles.kanbanBoard
            }

            state.columns.forEach { column ->
                console.log("Outputting column $column")

                styledUl {
                    css {
                        +KanbanStyles.kanbanColumn
                    }

                    styledLi {
                        css {
                            +KanbanStyles.columnHeader
                        }

                        +column
                    }
                    hr {  }
                    state.tasks[column]?.forEach { task ->
                        styledLi {
                            css {
                                +KanbanStyles.kanbanCard
                            }

                            styledDiv {
                                +task.description
                            }
                            if (task.due != null) {
                                styledDiv {
                                    +"Due : ${task.due}"
                                }
                            }
                            styledUl {
                                task.tags.forEach { tag ->
                                    styledLi {
                                        +tag
                                    }
                                }
                            }
                            styledUl {
                                task.dataviewFields.forEach { (field, value) ->
                                    +"$field : $value"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
