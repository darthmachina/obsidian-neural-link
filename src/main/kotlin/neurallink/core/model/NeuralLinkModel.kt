package neurallink.core.model

import NeuralLinkPlugin
import neurallink.core.settings.NeuralLinkPluginSettings6
import neurallink.core.store.FilterValue

/**
 * Global data store. Immutable and meant to be used with Redux.
 */
data class NeuralLinkModel(
    val plugin: NeuralLinkPlugin,
    val settings: NeuralLinkPluginSettings6,
    val tasks: List<Task>,
    val kanbanColumns: Map<StatusTag,List<Task>>,
    val filterValue: FilterValue<out Any>,
    val sourceFiles: List<String>
)
