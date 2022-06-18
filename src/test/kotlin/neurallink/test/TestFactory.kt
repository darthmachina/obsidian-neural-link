package neurallink.test

import neurallink.core.model.*
import kotlin.random.Random

class TestFactory {
    companion object {
        private val charPool : List<Char> = ('a'..'z') + ('A'..'Z')
        private fun randomString(length: Int) = (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")


        fun createTask(position: Int = -1) : Task {
            return Task(
                TaskFile(randomString(10) + ".md"),
                FilePosition(if (position == -1) Random.nextInt(0, 100) else position),
                Description(randomString(20)),
                null,
                null,
                emptySet(),
                emptyMap<DataviewField,DataviewValue<out Comparable<*>>>().toDataviewMap(),
                false,
                emptyList(),
                emptyList(),
                null,
                null
            )
        }

        fun createNote(position: Int = -1) : Note {
            return Note(
                randomString(20),
                FilePosition(if (position == -1) Random.nextInt(0, 100) else position)
            )
        }

        fun createDataviewPosition(pos: Double) : Pair<DataviewField,DataviewValue<Double>> {
            return Pair(
                DataviewField(TaskConstants.TASK_ORDER_PROPERTY),
                DataviewValue(pos)
            )
        }
    }
}