package neurallink.core.service

sealed class NeuralLinkError(open val message: String) {
    override fun toString(): String {
        return message
    }
}

class TaskReadingError(message: String) : NeuralLinkError(message)
class TaskReadingWarning(message: String) : NeuralLinkError(message)
class TaskWritingError(message: String) : NeuralLinkError(message)
class TaskWritingWarning(message: String) : NeuralLinkError(message)
class NotARepeatingTaskError(message: String) : NeuralLinkError(message)
class RepeatTaskParseError(message: String) : NeuralLinkError(message)
class DataviewFieldDoesNotExist(message: String) : NeuralLinkError(message)
