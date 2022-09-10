package neurallink.core.event

import App
import TFile
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import neurallink.core.model.NeuralLinkModel
import neurallink.core.model.TaskFile
import neurallink.core.service.changedTasks
import neurallink.core.service.pathInPathList
import neurallink.core.service.readFile
import neurallink.core.store.FileCreated
import neurallink.core.store.FileDeleted
import neurallink.core.store.ModifyFileTasks
import org.reduxkotlin.Store

private val logger = KotlinLogging.logger("NeuralLinkPlugin")

enum class FileEventType(val eventName: String) {
    EVENT_MODIFIED("changed"),
    EVENT_CREATED("created"),
    EVENT_DELETED("deleted")
}

val fileEventChannel = Channel<FileEvent>()

sealed class FileEvent(open val file: TFile)
data class FileEventModified(override val file: TFile) : FileEvent(file)
data class FileEventCreated(override val file: TFile) : FileEvent(file)
data class FileEventDeleted(override val file: TFile) : FileEvent(file)

suspend fun processFileEvents(
    app: App,
    store: Store<NeuralLinkModel>
) {
    for (fileEvent in fileEventChannel) {
        if (pathInPathList(fileEvent.file.path, store.state.settings.ignorePaths)) {
            // Ignore files that exist in ignorePaths
            continue
        }

        logger.debug { "Processing $fileEvent" }
        when (fileEvent) {
            is FileEventModified -> {
                val tasks = readFile(store, fileEvent.file, app.vault, app.metadataCache)
                if (changedTasks(fileEvent.file.path, tasks, store.state.tasks).isDefined()) {
                    store.dispatch(ModifyFileTasks(TaskFile(fileEvent.file.path), tasks))
                }
            }
            is FileEventCreated -> {
                val tasks = readFile(store, fileEvent.file, app.vault, app.metadataCache)
                if (changedTasks(fileEvent.file.path, tasks, store.state.tasks).isDefined()) {
                    store.dispatch(FileCreated(TaskFile(fileEvent.file.path), tasks))
                }
            }
            is FileEventDeleted -> {
                store.dispatch(FileDeleted(TaskFile(fileEvent.file.path)))
            }
        }
    }
}