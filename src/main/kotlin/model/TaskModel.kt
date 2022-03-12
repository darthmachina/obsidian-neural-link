package model

import NeuralLinkPluginSettings

/**
 * Right now this just holds a full list of all of the Tasks in the Vault.
 *
 * The idea is that there will be helper properties and methods to work with
 * this list.
 */
data class TaskModel(
    val settings: NeuralLinkPluginSettings,
    val tasks: MutableList<Task> = mutableListOf(),
    val kanbanColumns: MutableMap<String,MutableList<Task>> = mutableMapOf()
) {
    init {
        // Populate the map with the columng tags we are using
        // TODO Update this map if the columnTags value changes in settings
        settings.columnTags.forEach {
            kanbanColumns[it] = mutableListOf()
        }
    }
}