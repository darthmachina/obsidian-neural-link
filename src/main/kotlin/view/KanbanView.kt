package view

import ItemView
import Task
import Welcome
import WorkspaceLeaf
import kotlinx.browser.document
import react.Props
import react.dom.render
import kotlin.js.Promise

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
data class Board(val columns: Array<Column>)

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
data class ColumnList(val columns: Array<Column>)

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
data class Column(val id: Int, val title: String, val cards: Array<Card>)

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
data class Card(val id: Int, val title: String, val description: String)

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class KanbanView(leaf: WorkspaceLeaf) : ItemView(leaf) {
    companion object {
        val VIEW_TYPE = "NEURAL-LINK-KANBAN-VIEW"
    }

    init {
        console.log("KanbanView.init")
    }

    override fun getViewType(): String {
        return VIEW_TYPE
    }

    override fun getDisplayText(): String {
        return "Neural Link"
    }

    override fun onOpen(): Promise<Unit> {
        console.log("onOpen")

        val startBoard = Board(arrayOf(
            Column(1, "Backlog", arrayOf(Card(1, "Test 1", "Testing"))),
            Column(2, "In Progress", arrayOf(Card(2, "Test 2", "Working")))
        ))
        console.log("Initial Board: $startBoard")

        kotlinext.js.require("@asseinfo/react-kanban/dist/styles.css")
        render(this.contentEl) {
            ReactKanban {
                attrs {
                    initialBoard = startBoard
                }
            }
        }

        console.log("onOpen return")
        // Need a return
        return Promise { _: (Unit) -> Unit, _: (Throwable) -> Unit -> }
    }

    private fun createRandomTasks(start: Int, total: Int) : List<Task> {
        val tasks = mutableListOf<Task>()

        for (i in start until start+total) {
            tasks.add(Task(
                "full",
                "Task $i",
                null,
                null,
                mutableListOf("Tag"),
                mutableMapOf(Pair("Field", "Value")),
                false
            ))
        }

        return tasks
    }
}
