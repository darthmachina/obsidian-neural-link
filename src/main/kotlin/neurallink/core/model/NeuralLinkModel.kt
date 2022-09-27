package neurallink.core.model

import NeuralLinkPlugin
import arrow.core.None
import arrow.core.Option
import neurallink.core.settings.NeuralLinkPluginSettings6
import neurallink.core.store.DataviewFilterValue
import neurallink.core.store.FileFilterValue
import neurallink.core.store.TagFilterValue

enum class StoreActions {
    NOOP,
    UPDATE_SETTINGS,
    UPDATE_COLUMNS,
    UPDATE_FILTER,
    UPDATE_TASKS
}

/**
 * Global data store. Immutable and meant to be used with Redux.
 */
data class NeuralLinkModel(
    val modelLoaded: Boolean,
    val plugin: NeuralLinkPlugin,
    val settings: NeuralLinkPluginSettings6,
    val tasks: List<Task>,
    val kanbanColumns: Map<StatusTag,List<Task>>,
    val filterOptions: FilterOptions,
    val sourceFiles: List<String>,
    val latestAction: StoreActions
)

/**
 * Filter options. These are AND'd together when filtering tasks.
 */
data class FilterOptions(
    val tags: Option<TagFilterValue> = None,
    val page: Option<FileFilterValue> = None,
    val dataview: Option<DataviewFilterValue> = None,
    val hideFuture: Boolean = false
)
