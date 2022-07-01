package neurallink.core.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("unused")
class TaskFilterFunctionsTest : StringSpec({
    "pathInPathList finds exact match" {
        val pathToCheck = "test"
        val pathList = listOf("test")
        val actualCheck = pathInPathList(pathToCheck, pathList)
        actualCheck shouldBe true
    }

    "pathInPathList finds partial match" {
        val pathToCheck = "test/foo"
        val pathList = listOf("test")
        val actualCheck = pathInPathList(pathToCheck, pathList)
        actualCheck shouldBe true
    }

    "pathInPathList works with multiple paths in list" {
        val pathToCheck = "test"
        val pathList = listOf("test", "foo")
        val actualCheck = pathInPathList(pathToCheck, pathList)
        actualCheck shouldBe true
    }

    "pathInPathList returns false if no match" {
        val pathToCheck = "test"
        val pathList = listOf("foo")
        val actualCheck = pathInPathList(pathToCheck, pathList)
        actualCheck shouldBe false
    }

    "pathInPathList returns false if path contains but does not start with path in list" {
        val pathToCheck = "foo/test"
        val pathList = listOf("test")
        val actualCheck = pathInPathList(pathToCheck, pathList)
        actualCheck shouldBe false
    }
})
