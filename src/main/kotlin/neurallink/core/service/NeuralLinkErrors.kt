package neurallink.core.service

sealed class Error(open val message: String) {
    override fun toString(): String {
        return message
    }
}

class TaskReadingError(message: String) : Error(message)
class TaskReadingWarning(message: String) : Error(message)
class TaskWritingError(message: String) : Error(message)
class TaskWritingWarning(message: String) : Error(message)
class NotARepeatingTaskError(message: String) : Error(message)
class RepeatTaskParseError(message: String) : Error(message)
class DataviewFieldDoesNotExist(message: String) : Error(message)
