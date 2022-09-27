package neurallink.view

import WorkspaceLeaf
import io.kvision.html.Div
import io.kvision.panel.VPanel
import kotlinx.uuid.UUID
import mu.KotlinLogging
import neurallink.core.model.StatusTag
import neurallink.core.model.Task
import neurallink.core.model.NeuralLinkModel
import neurallink.core.model.StoreActions
import neurallink.core.model.TaskId
import neurallink.core.service.taskListEqualityWithTaskId
import neurallink.core.store.TaskMoved
import org.reduxkotlin.Store

private val logger = KotlinLogging.logger("KanbanBoard")

class KanbanBoard(
    val leaf: WorkspaceLeaf,
    val store: Store<NeuralLinkModel>
): VPanel() {
    companion object {
        const val CARD_MIME_TYPE = "text/x-card"
    }

    data class BoardCache(var tasks: MutableMap<StatusTag,List<Task>>)

    private var dragoverCardId: String? = null
    private val boardCache = BoardCache(mutableMapOf())
    private val columnsPanel = KanbanColumnsPanel()

    init {
        addCssStyle(KanbanStyles.ROOT)
        add(KanbanHeader(store))
        add(columnsPanel)
        updateCacheColumns(store.state.settings.columnTags, leaf)
        store.subscribe(::storeChanged)
    }

    private fun storeChanged() {
        logger.debug { "storeChanged()" }
        if (store.state.latestAction == StoreActions.UPDATE_COLUMNS) {
            updateCacheColumns(store.state.settings.columnTags, leaf)
        } else if (store.state.latestAction in listOf(StoreActions.UPDATE_TASKS, StoreActions.UPDATE_FILTER)){
            checkAndUpdateTasks()
        }
    }

    private fun updateCacheColumns(columns: List<StatusTag>, leaf: WorkspaceLeaf) {
        logger.debug { "updateCacheColumns(): $columns" }
        boardCache.tasks.clear()

        if (store.state.kanbanColumns.isNotEmpty()) {
            boardCache.tasks.putAll(store.state.kanbanColumns)

            columnsPanel.removeAll()
            columns.forEach { statusTag ->
                columnsPanel.addColumn(statusTag, createColumn(statusTag, boardCache.tasks[statusTag]!!, leaf))
            }
        }
    }

    private fun checkAndUpdateTasks() {
        logger.debug { "checkAndUpdateTasks()" }
        store.state.settings.columnTags.forEach { status ->
            val cacheTasks = boardCache.tasks[status]!!
            val storeTasks = store.state.kanbanColumns[status]!!
            if (!taskListEqualityWithTaskId(cacheTasks, storeTasks)) {
                boardCache.tasks[status] = storeTasks
                columnsPanel.replaceCards(
                    status,
                    boardCache.tasks[status]!!.map { createCard(it, status, leaf) }
                )
            }
        }
    }

    private fun createColumn(name: StatusTag, cards: List<Task>, leaf: WorkspaceLeaf): KanbanColumnPanel {
        logger.debug { "KanbanBoard.createColumn(): $name" }
        val column = KanbanColumnPanel(name, cards.map { createCard(it, name, leaf) })
        column.setDropTargetData(CARD_MIME_TYPE) { cardId ->
            if (cardId != null) {
                store.dispatch(TaskMoved(TaskId(UUID(cardId)), column.status, if (dragoverCardId == null) null else TaskId(UUID(dragoverCardId!!))))
            }
        }

        return column
    }

    private fun createCard(task: Task, status: StatusTag, leaf: WorkspaceLeaf): KanbanCardPanel {
        logger.debug { "createCard(): ${task.description}" }
        val card = KanbanCardPanel(leaf, store, task, status)
        card.id = task.id.value.toString()
        card.setDragDropData(CARD_MIME_TYPE, card.id!!)
        card.setEventListener<Div> {
            dragover = {
                dragoverCardId = card.id
                card.addCssStyle(KanbanStyles.DRAG_OVER_CARD)
            }

            dragleave = {
                dragoverCardId = null
                card.removeCssStyle(KanbanStyles.DRAG_OVER_CARD)
            }
        }

        return card
    }
}
