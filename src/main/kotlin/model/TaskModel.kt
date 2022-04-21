package model

import NeuralLinkPluginSettings
import neurallink.core.model.StatusTag
import neurallink.core.model.Task
import store.FilterValue

/**
 * Right now this just holds a full list of all the Tasks in the Vault.
 *
 * The idea is that there will be helper properties and methods to work with
 * this list.
 */
data class TaskModel(
    val settings: NeuralLinkPluginSettings,
    val tasks: List<Task>,
    val kanbanColumns: Map<StatusTag,List<Task>>,
    val filterValue: FilterValue<out Any>
)
