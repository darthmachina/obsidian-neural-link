package view


import io.kvision.html.button
import io.kvision.html.div
import io.kvision.panel.SimplePanel
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel

data class DialogResult(val accept: Boolean)

enum class DialogIcon {
    NONE,
    QUESTION
}

enum class DialogInput {
    NONE,
    SELECT
}

class Dialog(
    title: String,
    text: String,
    icon: DialogIcon = DialogIcon.NONE,
    input: DialogInput = DialogInput.NONE
): SimplePanel() {
    private var callback: (DialogResult) -> Unit = {}

    init {
        div {
            addCssStyle(DialogStyles.DIALOG)
            vPanel {
                addCssStyle(DialogStyles.DIALOG_BOX)
                hPanel {
                    addCssStyle(DialogStyles.DIALOG_HEADER)
                    +title
                    div {
                        addCssStyle(DialogStyles.DIALOG_CLOSE)
                        button("X").onClick {
                            callback.invoke(DialogResult(false))
                        }
                    }
                    div {
                        addCssStyle(DialogStyles.DIALOG_CONFIRM)
                        button("Save").onClick {
                            callback.invoke(DialogResult(true))
                        }
                    }
                }
                div {
                    addCssStyle(DialogStyles.DIALOG_CONTENT)
                    +text
                }
            }
        }
        hide()
    }

    fun setCallback(callback: (DialogResult) -> Unit) {
        this.callback = callback
    }

    fun show(show: Boolean) {
        console.log("Dialog.show(${show})")
        if (show) {
            show()
        } else {
            hide()
        }
    }
}