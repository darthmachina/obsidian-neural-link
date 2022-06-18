package neurallink.core.model

import NeuralLinkPlugin
import NeuralLinkPluginSettings5
import neurallink.core.store.FilterValue

/**
 * Global data store. Immutable and meant to be used with Redux.
 */
data class NeuralLinkModel(
    val plugin: NeuralLinkPlugin,
    val settings: NeuralLinkPluginSettings5,
    val tasks: List<Task>,
    val kanbanColumns: Map<StatusTag,List<Task>>,
    val filterValue: FilterValue<out Any>
)
