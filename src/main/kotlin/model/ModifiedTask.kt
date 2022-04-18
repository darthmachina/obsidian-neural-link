package model

import neurallink.core.model.Task

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED", "NON_EXPORTABLE_TYPE")
@OptIn(ExperimentalJsExport::class)
@JsExport
data class ModifiedTask(
    var original: Task,
    val before: MutableList<Task> = mutableListOf(),
    val after: MutableList<Task> = mutableListOf(),
    var modified: Boolean = false
)
