package neurallink.core.model

import arrow.core.Either
import arrow.core.right
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.uuid.UUID

@Serializable
abstract class ValueClass<T>(@Transient open val value: T? = null) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable data class TaskFile(override val value: String) : ValueClass<String>(value)
@Serializable data class FilePosition(override val value: Int) : ValueClass<Int>(value)
@Serializable data class Description(override val value: String) : ValueClass<String>(value)
@Serializable data class Tag(override val value: String) : ValueClass<String>(value)
@Serializable data class DueOn(override val value: LocalDate) : ValueClass<LocalDate>(value)
@Serializable data class CompletedOn(override val value: LocalDate) : ValueClass<LocalDate>(value)
@Serializable data class TaskId(override val value: UUID) : ValueClass<UUID>(value)
@Serializable data class DataviewField(override val value: String) : ValueClass<String>(value), Comparable<DataviewField> {
    override fun compareTo(other: DataviewField): Int {
        return value.compareTo(other.value)
    }
}
@Serializable data class DataviewValue(@Contextual override val value: Any) : ValueClass<Any>(value) {
    fun asDouble() : Either<String, Double> {
        return when (value) {
            is Double -> Either.Right(value)
            else -> Either.Left("value is not a Double")
        }
    }
}
@Serializable data class DataviewPair(@Contextual override val value: Pair<DataviewField, DataviewValue>) : ValueClass<Pair<DataviewField, DataviewValue>>(value)
@Serializable class DataviewMap() : HashMap<DataviewField,DataviewValue>() {
    constructor(original: Map<DataviewField,DataviewValue>) : this() {
        putAll(original)
    }


    fun copy() : DataviewMap {
        val newMap = DataviewMap()
        newMap.putAll(this)
        return newMap
    }

    fun valueForField(field: DataviewField) : Either<String,DataviewValue> {
        return get(field)?.right() ?: Either.Left("Field $field does not exist")
    }
}
