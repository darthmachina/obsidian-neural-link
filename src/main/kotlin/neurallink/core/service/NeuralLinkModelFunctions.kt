package neurallink.core.service

import arrow.core.Either
import arrow.core.right
import mu.KotlinLogging
import neurallink.core.model.Task
import neurallink.core.model.TaskFile

private val logger = KotlinLogging.logger("NeuralLinkModelFunctions")

fun removeTasksForFile(tasks: List<Task>, file: TaskFile): Either<NeuralLinkError, List<Task>> {
    logger.debug { "removeTasksForFile() : $file" }
    return tasks.filter { task -> task.file != file }.right()
}
