package service

import MetadataCache
import TFile
import Vault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.TaskModel
import neurallink.core.model.*
import neurallink.core.service.processAllFiles
import org.reduxkotlin.Store
import store.VaultLoaded

/**
 * Service for interacting with a TaskModel. Main use is to process files
 * in the Vault to build up the list of tasks and to process modified/created
 * files to incorporate those changes into the vault.
 */
class TaskModelService(val store: Store<TaskModel>) {
    fun loadTasKModelIntoStore(vault: Vault, metadataCache: MetadataCache, store: Store<TaskModel>) {
        CoroutineScope(Dispatchers.Main).launch {
            store.dispatch(VaultLoaded(processAllFiles(store, vault, metadataCache)))
        }
    }

    suspend fun writeModifiedTasks(tasks: List<Task>, vault: Vault) {
        console.log("writeModifiedTasks()", tasks)
        withContext(CoroutineScope(Dispatchers.Main).coroutineContext) {
            tasks
                .filter { it.original != null }
                .groupBy { it.file }
                .forEach { entry ->
                    launch {
                        val file = vault.getAbstractFileByPath(entry.key.value) as TFile
                        vault.read(file).then { contents ->
                            writeFile(vault, contents, entry.value, file)
                        }
                    }
                }
        }
    }

    private fun writeFile(vault: Vault, existingContents: String, tasks: List<Task>, file: TFile) {
        console.log("writeFile(): ${file.name}")
        val linesToRemove = mutableListOf<Int>()
        val fileContents = mutableListOf<String>().apply {
            addAll(existingContents.split('\n'))
        }
        var fileModified = false
        tasks
            .sortedByDescending { it.filePosition.value }
            .forEach { task ->
//                console.log(" - Updating task : ${task.description}")
                fileContents[task.filePosition.value] = task.toMarkdown()
                val indentedCount = indentedCount(task.original!!)
                if (indentedCount > 0) {
                    val firstIndent = task.filePosition.value + 1
                    // Use 'until' as we don't include the last element (indentedCount includes the firstIndent line)
                    linesToRemove.addAll((firstIndent until (firstIndent + indentedCount)).toList())
//                    console.log(" - linesToRemove now", linesToRemove)
                }
//                task.original = null
                fileModified = true
            }
        linesToRemove.sortedDescending().forEach {
            fileContents.removeAt(it)
        }

        if (fileModified) {
            vault.modify(file, fileContents.joinToString("\n"))
        }
    }

    /**
     * Recursive method to get the number of indented items.
     *
     * TODO: Does not handle notes with subnotes
     */
    private fun indentedCount(task: Task) : Int {
        return if (task.subtasks.isEmpty() && task.notes.isEmpty()) {
            0
        } else {
            task.subtasks.size +
                task.notes.size +
                task.subtasks.fold(0) { accumulator, subtask ->
                    accumulator + indentedCount(subtask)
                }
        }
    }
}
