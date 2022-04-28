@file:Suppress("unused")

package neurallink.core.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Month

class TaskStringFunctionsTest : StringSpec({
    "tests should run" {
        1 + 2 shouldBe 3
    }

    "getDueDate should parse correctly" {
        val testTask = "- [ ] Testing due dates @due(2022-01-02)"
        val maybeLocalDate = getDueDate(testTask)
        maybeLocalDate.isLeft() shouldBe false
        maybeLocalDate.isRight() shouldBe true
        val localDate = maybeLocalDate.orNull()
        localDate shouldNotBe null
        localDate!!.year shouldBe 2022
        localDate.month shouldBe Month.JANUARY
        localDate.dayOfMonth shouldBe 2
    }
})
