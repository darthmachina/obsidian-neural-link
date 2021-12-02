package event

import NeuralLinkPlugin
import NeuralLinkState
import TFile
import processor.RemoveRegexFromTask

@OptIn(ExperimentalJsExport::class)
@JsExport
class FileModifiedEvent(plugin: NeuralLinkPlugin, state: NeuralLinkState) : Event(plugin) {
    @Suppress("NON_EXPORTABLE_TYPE")
    val taskProcessors = listOf(RemoveRegexFromTask(state))

    override fun processEvent(context: Any) {
        console.log("processEvent: ", context)
        if (context is TFile) {
            var modified = false
            val fileContents = mutableListOf<String>()
            plugin.app.vault.read(context).then { contents ->
                fileContents.addAll(contents.split('\n'))
                val fileListItems = plugin.app.metadataCache.getFileCache(context)?.listItems ?: arrayOf()
                fileListItems.forEach { listItem ->
                    if (listItem.task?.uppercase() == "X") {
                        var lineContents = fileContents[listItem.position.start.line.toInt()]
                        // Pass the task line through all of the configured TaskProcessors
                        taskProcessors.forEach { processor ->
                            lineContents = processor.processTask(lineContents)
                        }

                        if (lineContents != fileContents[listItem.position.start.line.toInt()]) {
                            fileContents[listItem.position.start.line.toInt()] = lineContents
                            // Only mark as modified if the line was changed in some way
                            modified = true
                        }
                    }
                }

                if (modified) {
                    plugin.app.vault.modify(context, fileContents.joinToString("\n"))
                }
            }
        }
    }
}
