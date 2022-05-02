package neurallink.core.model

import arrow.core.Either
import arrow.core.right
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.uuid.UUID
import neurallink.core.service.DataviewFieldDoesNotExist
import service.TagSerializer

sealed class ValueClass<T : Comparable<T>>(@Transient open val value: T? = null) : Comparable<T> {
    override fun compareTo(other: T): Int {
        return value?.compareTo(other) ?: -1
    }
}

data class TaskFile(override val value: String) : ValueClass<String>(value)

data class FilePosition(override val value: Int) : ValueClass<Int>(value)

data class Description(override val value: String) : ValueClass<String>(value)

@Serializable(with = TagSerializer::class)
data class Tag(override val value: String) : ValueClass<String>(value) {
    override fun toString(): String {
        return value
    }
}

data class DueOn(override val value: LocalDate) : ValueClass<LocalDate>(value)

data class CompletedOn(override val value: LocalDateTime) : ValueClass<LocalDateTime>(value)

data class TaskId(override val value: UUID) : ValueClass<UUID>(value)

data class DataviewField(override val value: String) : ValueClass<String>(value)

data class DataviewValue<T : Comparable<T>>(@Contextual override val value: T) : ValueClass<T>(value) {
    fun asDouble(): Double {
        return when (value) {
            is Double -> value
            else -> throw IllegalStateException("value is not a double: ${value::class}")
        }
    }

    fun asString(): String {
        return when (value) {
            is String -> value
            else -> throw IllegalStateException("value is not a string: ${value::class}")
        }
    }
}

data class DataviewPair<T : Comparable<T>>(@Contextual val value: Pair<DataviewField, DataviewValue<T>>)

class DataviewMap() : HashMap<DataviewField, DataviewValue<out Comparable<*>>>() {
    constructor(original: Map<DataviewField, DataviewValue<out Comparable<*>>>) : this() {
        putAll(original)
    }

    fun copy(): DataviewMap {
        val newMap = DataviewMap()
        newMap.putAll(this)
        return newMap
    }

    fun valueForField(field: DataviewField): Either<DataviewFieldDoesNotExist, DataviewValue<out Comparable<*>>> {
        return get(field)?.right() ?: Either.Left(DataviewFieldDoesNotExist("Field $field does not exist, ${this.keys}"))
    }
}

fun Map<DataviewField,DataviewValue<out Comparable<*>>>.toDataviewMap() : DataviewMap {
    return DataviewMap(this)
}
