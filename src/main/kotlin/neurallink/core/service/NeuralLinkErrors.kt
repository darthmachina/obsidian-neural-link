package neurallink.core.service

sealed class Error(open val message: String)

class TaskReadingError(message: String) : Error(message)
class TaskReadingWarning(message: String) : Error(message)