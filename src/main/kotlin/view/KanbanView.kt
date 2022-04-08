package view

import ItemView
import WorkspaceLeaf
import io.kvision.*
import io.kvision.html.div
import io.kvision.panel.root
import model.TaskModel
import org.reduxkotlin.Store
import service.RepeatingTaskService
import service.TaskModelService
import kotlin.js.Promise

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class KanbanView(leaf: WorkspaceLeaf, val store: Store<TaskModel>, val repeatingTaskService: RepeatingTaskService) : ItemView(leaf) {
    companion object {
        const val VIEW_TYPE = "NEURAL-LINK-KANBAN-VIEW"
    }

    init {
        console.log("KanbanView.init()")
    }

    override fun getViewType(): String {
        return VIEW_TYPE
    }

    override fun getDisplayText(): String {
        return "Neural Link"
    }

    override fun onOpen(): Promise<Unit> {
        console.log("KanbanView.onOpen()")

        startApplication(
            ::KanbanApp,
            null,
            CoreModule
        )
        // Need a return
        return Promise { _: (Unit) -> Unit, _: (Throwable) -> Unit -> }
    }

    inner class KanbanApp: Application() {
        override fun start() {
            root(contentEl) {
                addCssStyle(KanbanStyles.ROOT)
                add(KanbanBoard(store, repeatingTaskService))
            }
        }
    }
}
