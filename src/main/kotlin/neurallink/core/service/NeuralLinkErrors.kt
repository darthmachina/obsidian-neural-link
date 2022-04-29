package neurallink.core.service

sealed class Error(open val message: String) {
    override fun toString(): String {
        return message
    }
}

class TaskReadingError(message: String) : Error(message)
class TaskReadingWarning(message: String) : Error(message)