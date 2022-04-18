package view

import WorkspaceLeaf
import io.kvision.html.Div
import io.kvision.panel.VPanel
import model.StatusTag
import neurallink.core.model.Task
import model.TaskModel
import org.reduxkotlin.Store
import service.RepeatingTaskService
import store.TaskMoved

class KanbanBoard(
    val leaf: WorkspaceLeaf,
    val store: Store<TaskModel>,
    private val repeatingTaskService: RepeatingTaskService
): VPanel() {
    companion object {
        const val CARD_MIME_TYPE = "text/x-card"
    }

    data class BoardCache(var columns: List<StatusTag>, var tasks: MutableMap<StatusTag,List<Task>>)

    private var dragoverCardId: String? = null
    private val boardCache = BoardCache(mutableListOf(), mutableMapOf())
    private val columnsPanel = KanbanColumnsPanel()

    init {
        addCssStyle(KanbanStyles.ROOT)
        add(KanbanHeader(store))
        add(columnsPanel)
        // If the columns are different update the board
        if (boardCache.columns != store.state.settings.columnTags) {
            updateCacheColumns(store.state.settings.columnTags, leaf)
        }
        store.subscribe(::storeChanged)
    }

    private fun storeChanged() {
        console.log("KanbanBoard.storeChanged()")
        if (boardCache.columns != store.state.settings.columnTags) {
            // Columns have been updated, redraw the whole board
            updateCacheColumns(store.state.settings.columnTags, leaf)
        } else {
            // If columns match, just check task list
            // Check each column for whether the cache matches the store
            // First, check if any columns were removed
//            removeOldColumns()
            // Second, check if the task list is the same for each column
            checkAndUpdateTasks()
        }
    }

    private fun updateCacheColumns(columns: List<StatusTag>, leaf: WorkspaceLeaf) {
        console.log("KanbanBoard.updateCacheColumns(): ", columns)
        boardCache.columns = columns
        boardCache.tasks.clear()
        boardCache.tasks.putAll(store.state.kanbanColumns)

        columnsPanel.removeAll()
        boardCache.columns.forEach { statusTag ->
//            console.log(" - creating column", statusTag)
            columnsPanel.addColumn(statusTag, createColumn(statusTag, boardCache.tasks[statusTag]!!, leaf))
        }
    }

    private fun checkAndUpdateTasks() {
        console.log("KanbanBoard.checkAndUpdateTasks()")
        store.state.settings.columnTags.forEach { status ->
//            console.log(" - Checking column: ", status)
            val cacheTasks = boardCache.tasks[status]!!
            val storeTasks = store.state.kanbanColumns[status]!!
            if (cacheTasks != storeTasks) {
//                console.log(" - Task difference between cache and store, updating")
                boardCache.tasks[status] = storeTasks
                columnsPanel.replaceCards(
                    status,
                    boardCache.tasks[status]!!.map { createCard(it, status, leaf) }
                )
            }
        }
    }

    private fun createColumn(name: StatusTag, cards: List<Task>, leaf: WorkspaceLeaf): KanbanColumnPanel {
        console.log("KanbanBoard.createColumn(): ", name)
        val column = KanbanColumnPanel(name, cards.map { createCard(it, name, leaf) })
        column.setDropTargetData(CARD_MIME_TYPE) { cardId ->
            if (cardId != null) {
                store.dispatch(TaskMoved(cardId, column.status.tag, dragoverCardId))
            }
        }

        return column
    }

    private fun createCard(task: Task, status: StatusTag, leaf: WorkspaceLeaf): KanbanCardPanel {
        console.log("KanbanBoard.createCard(): ", task.description)
        val card = KanbanCardPanel(leaf, store, task, status, repeatingTaskService)
        card.id = task.id
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
