package view

import Command
import ItemView
import Welcome
import WorkspaceLeaf
import kotlinx.browser.document
import react.dom.render
import kotlin.js.Promise

class KanbanView(leaf: WorkspaceLeaf) : ItemView(leaf) {
    companion object {
        val VIEW_TYPE = "NEURAL-LINK-KANBAN-VIEW"
    }

    override fun getViewType(): String {
        return VIEW_TYPE
    }

    override fun getDisplayText(): String {
        return "Neural Link"
    }

    override fun onOpen(): Promise<Unit> {
        val rootElement = document.createElement("div")
        render(rootElement) {
            child(Welcome::class) {
                attrs {
                    name = "Kotlin/JS"
                }
            }
        }

        // Need a return
        return Promise() { _: (Unit) -> Unit, _: (Throwable) -> Unit -> }
    }
}
