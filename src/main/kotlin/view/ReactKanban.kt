@file:JsModule("@asseinfo/react-kanban")
@file:JsNonModule
package view

import react.ComponentClass
import react.Props

@JsName("default")
external val ReactKanban: ComponentClass<ReactKanbanProps>

external interface ReactKanbanProps : Props {
    var board: KanbanView.Board
}
