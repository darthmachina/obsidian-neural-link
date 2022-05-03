package neurallink.test

import neurallink.core.model.DataviewField
import neurallink.core.model.DataviewValue
import neurallink.core.model.Description
import neurallink.core.model.FilePosition
import neurallink.core.model.Note
import neurallink.core.model.Task
import neurallink.core.model.TaskFile
import neurallink.core.model.toDataviewMap
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
    }
}