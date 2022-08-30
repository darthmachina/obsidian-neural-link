package neurallink.view

import io.kvision.core.AlignItems
import io.kvision.html.div
import io.kvision.panel.VPanel
import io.kvision.panel.vPanel
import neurallink.core.model.StatusTag

class KanbanColumnPanel(val status: StatusTag, cards: List<KanbanCardPanel>): VPanel(spacing = 10, alignItems = AlignItems.CENTER) {
    private val cardPanel: VPanel

    init {
        addCssStyle(KanbanStyles.KANBAN_COLUMN)
        div {
            +status.displayName
        }
        cardPanel = vPanel(spacing = 10, alignItems = AlignItems.CENTER) {
            addCssStyle(KanbanStyles.KANBAN_CARD_LIST)
            cards.forEach { card ->
                add(card)
            }
        }
    }

    fun addCards(cards: List<KanbanCardPanel>) = cardPanel.addAll(cards)
    fun removeAllCards() = cardPanel.removeAll()
}
