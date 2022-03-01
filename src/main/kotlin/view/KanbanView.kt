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

        render(this.contentEl) {
            ReactKanban {
                attrs {
                    board = Board(listOf(
                        Column(1, "Backlog", listOf(Card(1, "Test 1", "Testing")))
                    ))
                }
            }
        }

        console.log("onOpen return")
        // Need a return
        return Promise { _: (Unit) -> Unit, _: (Throwable) -> Unit -> }
    }

    data class Board(val columns: List<Column>)

    data class Column(val id: Int, val title: String, val cards: List<Card>)

    data class Card(val id: Int, val title: String, val description: String)

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
