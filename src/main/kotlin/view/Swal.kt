
package view

@JsModule("sweetalert2")
@JsNonModule
external class Swal {
    companion object {
        fun fire(
            title: String,
            text: String = definedExternally,
            icon: String = definedExternally,
            input: String = definedExternally,
            inputOptions: Map<String,String> = definedExternally,
            showCancelButton: Boolean = definedExternally
        )
    }
}
