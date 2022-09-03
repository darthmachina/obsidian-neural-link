package neurallink.view

import ItemView
import WorkspaceLeaf
import io.kvision.*
import io.kvision.panel.root
import mu.KotlinLogging
import neurallink.core.model.NeuralLinkModel
import org.reduxkotlin.Store
import kotlin.js.Promise

private val logger = KotlinLogging.logger("KanbanView")

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

    override fun getViewType(): String {
        return VIEW_TYPE
    }

    override fun getDisplayText(): String {
        return "Neural Link"
    }

    override fun getIcon(): String {
        return "checkbox-glyph"
    }

    override fun onOpen(): Promise<Unit> {
        logger.debug { "KanbanView.onOpen()" }
        icon = "checkbox-glyph"
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
