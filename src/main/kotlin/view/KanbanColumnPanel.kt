package view

import io.kvision.core.AlignItems
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.panel.VPanel
import model.StatusTag

class KanbanColumnPanel(val status: StatusTag, cards: List<Div>): VPanel(spacing = 5, alignItems = AlignItems.CENTER) {
    init {
        addCssStyle(KanbanStyles.KANBAN_COLUMN)
        div {
            +status.displayName
        }
        cards.forEach { card ->
            add(card)
        }
    }

    /**
     * The header is at position 0, the cards are at position 1+.
     * We start at the end of the list and remove until only 1 is left
     */
    fun removeAllCards() {
        if (getChildren().size > 1) {
            for(i in getChildren().size - 1 downTo 1) {
                removeAt(i)
            }
        }
    }
}
