package model

import NeuralLinkPluginSettings

/**
 * Right now this just holds a full list of all the Tasks in the Vault.
 *
 * The idea is that there will be helper properties and methods to work with
 * this list.
 */
data class TaskModel(
    val settings: NeuralLinkPluginSettings,
    val tasks: MutableList<Task>,
    val kanbanColumns: MutableMap<String,MutableList<Task>>
) {
    init {
        // Populate the map with the column tags we are using if it's empty
        // TODO Update this map if the columnTags value changes in settings
        if (kanbanColumns.isEmpty()) {
            settings.columnTags.forEach {
                kanbanColumns[it] = mutableListOf()
            }
        }
    }
}
