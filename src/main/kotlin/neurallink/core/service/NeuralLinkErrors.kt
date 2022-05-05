package neurallink.core.service

sealed class NeuralLinkError(
    open val message: String,
    open val isError: Boolean = true,
    open val throwable: Throwable? = null
) {
    override fun toString(): String {
        return "$message ($throwable)"
    }
}

class LoadSettingsError(message: String, throwable: Throwable? = null) : NeuralLinkError(message, true, throwable)
class TaskReadingError(message: String, throwable: Throwable? = null) : NeuralLinkError(message, true, throwable)
class TaskReadingWarning(message: String, throwable: Throwable? = null) : NeuralLinkError(message, false, throwable)
class TaskWritingError(message: String, throwable: Throwable? = null) : NeuralLinkError(message, true, throwable)
class TaskWritingWarning(message: String, throwable: Throwable? = null) : NeuralLinkError(message, false, throwable)
class NotARepeatingTaskWarning(message: String, throwable: Throwable? = null) : NeuralLinkError(message, false, throwable)
class RepeatTaskParseError(message: String, throwable: Throwable? = null) : NeuralLinkError(message, true, throwable)
class DataviewFieldDoesNotExist(message: String, throwable: Throwable? = null) : NeuralLinkError(message, true, throwable)
class BeforeTaskDoesNotExist(message: String) : NeuralLinkError(message)
class NoStatusTagOnTaskWarning(message: String) : NeuralLinkError(message, false)