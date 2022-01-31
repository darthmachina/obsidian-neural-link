package view

import ItemView
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
            child(Welcome::class) {
                attrs {
                    name = "Kotlin/JS"
                }
            }
        }

        console.log("onOpen return")
        // Need a return
        return Promise { _: (Unit) -> Unit, _: (Throwable) -> Unit -> }
    }
}
