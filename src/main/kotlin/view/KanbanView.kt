package view

import ItemView
import Task
import Welcome
import WorkspaceLeaf
import kotlinx.browser.document
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
            child(Kanban::class) {
                attrs {
                    columns = listOf("Backlog", "In Progress", "Waiting", "Completed")
                    tasks = mapOf(
                        Pair("Backlog", createRandomTasks(1, 2)),
                        Pair("In Progress", createRandomTasks(3, 2)),
                        Pair("Waiting", createRandomTasks(5, 2)),
                        Pair("Completed", createRandomTasks(7, 2))
                    )
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
