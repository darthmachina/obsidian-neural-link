package neurallink.core.service

import kotlinx.datetime.LocalDate
import neurallink.core.model.DataviewField
import neurallink.core.model.Task
import neurallink.core.model.TaskConstants

val taskComparator = compareBy<Task,Double?>(nullsLast()) {
    val position = it.dataviewFields.valueForField(DataviewField(TaskConstants.TASK_ORDER_PROPERTY)).orNull()?.asDouble()
    position
}

val taskDateComparator = compareBy<Task, LocalDate?>(nullsFirst()) {
    it.dueOn?.value
}