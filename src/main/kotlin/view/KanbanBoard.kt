package view

import io.kvision.html.Div
import io.kvision.panel.HPanel
import io.kvision.panel.VPanel
import model.StatusTag
import model.Task
import model.TaskModel
import org.reduxkotlin.Store
import store.TaskMoved

class KanbanBoard(val store: Store<TaskModel>): HPanel() {
    companion object {
        const val CARD_MIME_TYPE = "text/x-card"
    }

    data class BoardCache(var columns: List<StatusTag>, var tasks: MutableMap<StatusTag,List<Task>>)

    private var dragoverCardId: String? = null
    private val boardCache = BoardCache(mutableListOf(), mutableMapOf())
    private val columnPanels = mutableMapOf<StatusTag,KanbanColumnPanel>()

    init {
        addCssStyle(KanbanStyles.ROOT)
        // If the columns are different update the board
        if (boardCache.columns != store.state.settings.columnTags) {
            updateCacheColumns(store.state.settings.columnTags)
        }
        store.subscribe(::storeChanged)
    }

    private fun storeChanged() {
        console.log("KanbanBoard.storeChanged()")
        if (boardCache.columns != store.state.settings.columnTags) {
            // Columns have been updated, redraw the whole board
            updateCacheColumns(store.state.settings.columnTags)
        } else {
            // If columns match, just check task list
            // Check each column for whether the cache matches the store
            // First, check if any columns were removed
//            removeOldColumns()
            // Second, check if the task list is the same for each column
            checkAndUpdateTasks()
        }
    }

    private fun updateCacheColumns(columns: List<StatusTag>) {
        console.log("KanbanBoard.updateCacheColumns(): ", columns)
        boardCache.columns = columns
        boardCache.tasks.clear()
        columnPanels.clear()
        boardCache.tasks.putAll(store.state.kanbanColumns)

        removeAll()
        boardCache.columns.forEach { statusTag ->
//            console.log(" - creating column", statusTag)
            add(createColumn(statusTag, boardCache.tasks[statusTag]!!))
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
                val column = columnPanels[status]!!
                column.removeAllCards()
                column.addCards(boardCache.tasks[status]!!.map { createCard(it, status.tag) })
            }
        }
    }

    private fun createColumn(name: StatusTag, cards: List<Task>): VPanel {
        console.log("KanbanBoard.createColumn(): ", name)
        val column = KanbanColumnPanel(name, cards.map { createCard(it, name.tag) })
        column.setDropTargetData(CARD_MIME_TYPE) { cardId ->
            if (cardId != null) {
                store.dispatch(TaskMoved(cardId, column.status.tag, dragoverCardId))
            }
        }
        columnPanels[name] = column

        return column
    }

    private fun createCard(task: Task, status: String): KanbanCardPanel {
        console.log("KanbanBoard.createCard(): ", task.description)
        val card = KanbanCardPanel(store, task, status)
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
