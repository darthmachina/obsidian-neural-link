package view

import io.kvision.core.*
import io.kvision.utils.auto
import io.kvision.utils.perc
import io.kvision.utils.px

class DialogStyles {
    companion object {
        val DIALOG = Style(".nl-modal") {
            display = Display.BLOCK
            position = Position.FIXED
            zIndex = 1
            left = 0.px
            top = 0.px
            width = 100.perc
            height = 100.perc
            overflow = Overflow.AUTO
            background = Background(color = Color.rgba(0, 0, 0, 0xaa))
            cursor = Cursor.DEFAULT
        }

        val DIALOG_BOX = Style(".nl-modal-content") {
            background = Background(color = Color.name(Col.BLACK))
            colorName = Col.WHITE
            margin = auto
            marginTop = 15.perc
            border = Border(width = 1.px, BorderStyle.SOLID, Color.hex(0x888888))
            width = 80.perc
        }

        val DIALOG_HEADER = Style(".nl-dialog-header") {
            paddingTop = 0.px
            marginTop = 0.px
            marginBottom = 15.px
            height = 25.px
            width = 100.perc
            fontSize = 20.px
            fontWeight = FontWeight.BOLD
        }

        val DIALOG_CONTENT = Style(".nl-dialog-content") {
            textAlign = TextAlign.LEFT
            paddingLeft = 20.px
            paddingRight = 20.px
            width = 100.perc

            style("select") {
                width = 100.perc
                height = 25.px
                colorName = Col.WHITE
            }
        }

        val DIALOG_CONFIRM = Style("nl-dialog-confirm") {
            height = 30.px
            marginTop = 15.px

            style("button") {
                marginLeft = auto
                marginRight = 20.px
                marginBottom = 10.px
            }
        }

        val DIALOG_CLOSE = Style(".nl-dialog-close") {
            color = Color.hex(0xaaaaaa)
            marginLeft = auto
            top = 0.px
            right = 0.px
            fontSize = 22.px
            fontWeight = FontWeight.BOLD

            style("button") {
                marginRight = 0.px
            }
        }
    }
}