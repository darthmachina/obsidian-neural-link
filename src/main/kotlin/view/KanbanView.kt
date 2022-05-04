package view

import ItemView
import WorkspaceLeaf
import io.kvision.*
import io.kvision.panel.root
import model.NeuralLinkModel
import org.reduxkotlin.Store
import kotlin.js.Promise

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalJsExport::class)
@JsExport
class KanbanView(
    leaf: WorkspaceLeaf,
    val store: Store<NeuralLinkModel>
) : ItemView(leaf) {
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
            CoreModule,
            FontAwesomeModule
        )
        // Need a return
        return Promise { _: (Unit) -> Unit, _: (Throwable) -> Unit -> }
    }

    inner class KanbanApp: Application() {
        override fun start() {
            root(contentEl) {
                addCssStyle(KanbanStyles.ROOT)
                add(KanbanBoard(leaf, store))
            }
        }
    }
}
