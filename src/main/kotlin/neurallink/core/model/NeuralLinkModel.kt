package neurallink.core.model

import NeuralLinkPlugin
import NeuralLinkPluginSettings
import neurallink.core.model.StatusTag
import neurallink.core.model.Task
import neurallink.core.store.FilterValue

/**
 * Global data store. Immutable and meant to be used with Redux.
 */
data class NeuralLinkModel(
    val plugin: NeuralLinkPlugin,
    val settings: NeuralLinkPluginSettings,
    val tasks: List<Task>,
    val kanbanColumns: Map<StatusTag,List<Task>>,
    val filterValue: FilterValue<out Any>
)
