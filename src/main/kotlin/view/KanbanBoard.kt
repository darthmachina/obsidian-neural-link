package view

import io.kvision.html.Div
import io.kvision.panel.HPanel
import io.kvision.panel.VPanel
import model.Task
import model.TaskModel
import org.reduxkotlin.Store
import service.TaskModelService
import store.TaskStatusChanged

class KanbanBoard(val store: Store<TaskModel>, val taskModelService: TaskModelService): HPanel() {
    companion object {
        const val CARD_MIME_TYPE = "text/x-card"
    }

    data class BoardCache(val columns: MutableList<String>, val tasks: MutableMap<String,MutableList<Task>>)

    private var dragoverCardId: String? = null
    private val boardCache = BoardCache(mutableListOf(), mutableMapOf())
    private val columnPanels = mutableMapOf<String,KanbanColumnPanel>()

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
        store.state.kanbanColumns.keys.forEach { status ->
            console.log(" - task list for $status : ", store.state.kanbanColumns[status]!!)
        }
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

    private fun updateCacheColumns(columns: MutableList<String>) {
        console.log("updateCacheColumns(): ", columns)
        boardCache.columns.clear()
        boardCache.tasks.clear()
        columnPanels.clear()
        boardCache.columns += columns
        boardCache.tasks.putAll(store.state.kanbanColumns)

        removeAll()
        boardCache.tasks.forEach { (name, cards) ->
            console.log(" - creating column for $name")
            add(createColumn(name, cards))
        }
    }

    private fun checkAndUpdateTasks() {
        console.log("checkAndUpdateTasks()")
        store.state.settings.columnTags.forEach { status ->
            console.log(" - Checking column: ", status)
            val cacheTasks = boardCache.tasks[status]!!
            val storeTasks = store.state.kanbanColumns[status]!!
            if (cacheTasks != storeTasks) {
                console.log(" - Task difference between cache and store, updating")
                boardCache.tasks[status] = storeTasks
                val column = columnPanels[status]!!
                column.removeAllCards()
                column.addAll(boardCache.tasks[status]!!.map { createCard(it) })
            }
        }
    }

    private fun createColumn(name: String, cards: MutableList<Task>): VPanel {
        console.log("createColumn(): ", name)
        val column = KanbanColumnPanel(name, cards.map { createCard(it) })
        column.setDropTargetData(CARD_MIME_TYPE) { cardId ->
            if (cardId != null) {
                store.dispatch(TaskStatusChanged(cardId, column.status, dragoverCardId))
            }
        }
        columnPanels[name] = column

        return column
    }

    private fun createCard(task: Task): Div {
        console.log("createCard(): ", task.description)
        val card = Div(task.description)
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
