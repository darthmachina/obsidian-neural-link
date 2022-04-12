package view

import io.kvision.require
import react.ComponentClass
import react.PropsWithChildren

external interface SweetAlertProps : PropsWithChildren {
    var title: String
}

val SweetAlert: ComponentClass<SweetAlertProps> = require("sweetalert2") as ComponentClass<SweetAlertProps>

external fun fireSweetAlert(
    title: String = definedExternally,
    text: String = definedExternally,
    icon: String = definedExternally
)