package view


import io.kvision.core.StringPair
import io.kvision.form.FormInput
import io.kvision.form.select.SimpleSelectInput
import io.kvision.form.select.simpleSelectInput
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.panel.SimplePanel
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel

data class DialogResult(
    val accept: Boolean,
    val result: Any?
)

enum class DialogIcon {
    NONE,
    QUESTION
}

enum class DialogInput(var model: Any, var current: Any) {
    NONE(Unit, Unit),
    SELECT(listOf<StringPair>(), "" to ""),
    CUSTOM(Unit, Unit)
}

class Dialog(
    title: String,
    text: String,
    icon: DialogIcon = DialogIcon.NONE,
    input: DialogInput = DialogInput.NONE
): SimplePanel() {
    private var callback: (DialogResult) -> Unit = {}
    private var formComponent: FormInput? = null

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
                            callback.invoke(DialogResult(false, Unit))
                        }
                    }
                }
                div {
                    addCssStyle(DialogStyles.DIALOG_CONTENT)
                    div {
                        +text
                    }
                    div {
                        when (input) {
                            DialogInput.SELECT -> {
                                formComponent = simpleSelectInput(
                                    options = input.model as List<StringPair>,
                                    value = input.current as String
                                )
                            }
                        }
                    }
                }
                div {
                    addCssStyle(DialogStyles.DIALOG_CONFIRM)
                    button("Save").onClick {
                        val result = when(input) {
                            DialogInput.NONE -> null
                            DialogInput.SELECT -> {
                                (formComponent as SimpleSelectInput).value
                            }
                            DialogInput.CUSTOM -> {
                                console.log("custom dialog content")
                                null
                            }
                        }
                        callback.invoke(DialogResult(
                            true,
                            result
                        ))
                    }
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