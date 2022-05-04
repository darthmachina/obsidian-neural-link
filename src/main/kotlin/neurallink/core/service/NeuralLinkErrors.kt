package neurallink.core.service

sealed class NeuralLinkError(
    open val message: String,
    open val throwable: Throwable? = null
) {
    override fun toString(): String {
        return "$message ($throwable)"
    }
}

class TaskReadingError(message: String, throwable: Throwable? = null) : NeuralLinkError(message, throwable)
class TaskReadingWarning(message: String, throwable: Throwable? = null) : NeuralLinkError(message, throwable)
class TaskWritingError(message: String, throwable: Throwable? = null) : NeuralLinkError(message, throwable)
class TaskWritingWarning(message: String, throwable: Throwable? = null) : NeuralLinkError(message, throwable)
class NotARepeatingTaskError(message: String, throwable: Throwable? = null) : NeuralLinkError(message, throwable)
class RepeatTaskParseError(message: String, throwable: Throwable? = null) : NeuralLinkError(message, throwable)
class DataviewFieldDoesNotExist(message: String, throwable: Throwable? = null) : NeuralLinkError(message, throwable)
