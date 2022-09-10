package neurallink.test

import kotlinx.datetime.LocalDate
import neurallink.core.model.*
import kotlin.random.Random

class TestFactory {
    companion object {
        private val charPool : List<Char> = ('a'..'z') + ('A'..'Z')
        private fun randomString(length: Int) = (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")

        fun createTasks(number: Int) : List<Task> {
            return List(number) { createTask() }
        }

        /**
         * Create a fully populated Task.
         *
         * If more than one will be created future filePositions should account for the 3 rows of this Task.
         */
        fun createFullTask(filePosition: Int) : Task {
            return createTask(
                subtasks = listOf(createTask(position = filePosition + 1)),
                notes = listOf(Note(randomString(10), FilePosition(filePosition + 2))),
                tags = setOf(Tag(randomString(4))),
                dueOn = DueOn(LocalDate(2021, 1, 1))
            )
        }

        fun createTask(
            position: Int = -1,
            file: String = randomString(10) + ".md",
            description: String = randomString(20),
            subtasks: List<Task> = emptyList(),
            notes: List<Note> = emptyList(),
            tags: Set<Tag> = emptySet(),
            dueOn: DueOn? = null,
            dataviewFields: Map<DataviewField,DataviewValue<out Comparable<*>>> = emptyMap()
        ) : Task {
            return Task(
                TaskFile(file),
                FilePosition(if (position == -1) Random.nextInt(0, 100) else position),
                Description(description),
                dueOn,
                null,
                tags,
                dataviewFields.toDataviewMap(),
                false,
                subtasks,
                notes,
                null,
                null
            )
        }

        fun createNote(
            position: Int = -1,
            note: String = randomString(20)
        ) : Note {
            return Note(
                note,
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
