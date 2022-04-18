package neurallink.core.model

@OptIn(ExperimentalJsExport::class)
@JsExport
sealed class ListItem {
    abstract val filePosition: Int
}
