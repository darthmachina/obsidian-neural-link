package neurallink.core.view

import io.kvision.panel.FlexPanel
import io.kvision.panel.HPanel
import neurallink.core.model.StatusTag

class KanbanColumnsPanel : HPanel() {
    private val columnPanels = mutableMapOf<StatusTag, KanbanColumnPanel>()

    init {
        addCssStyle(KanbanStyles.KANBAN_COLUMNS)
    }

    fun addColumn(status: StatusTag, column: KanbanColumnPanel) {
        columnPanels[status] = column
        add(column)
    }

    override fun removeAll(): FlexPanel {
        columnPanels.clear()
        super.removeAll()
        return this
    }

    fun replaceCards(status: StatusTag, cards: List<KanbanCardPanel>) {
        val column = columnPanels[status]!!
        column.removeAllCards()
        column.addCards(cards)
    }
}
