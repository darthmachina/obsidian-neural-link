package neurallink.core.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import neurallink.test.TestListItemCache
import neurallink.test.TestLoc
import neurallink.test.TestPos

@Suppress("UNUSED_PARAMETER")
class VaultFunctionsTest : StringSpec ({
    // *** cacheItemParent() ***
    "cacheItemParent returns correct value" {
        val expectedParent = 5
        val listItemCache = TestListItemCache(
            expectedParent,
            TestPos(TestLoc.EMPTY, TestLoc.EMPTY)
        )

        val actualParent = cacheItemParent(listItemCache)
        actualParent shouldBe expectedParent
    }

    // *** cacheItemLine() ***
    "cacheItemLine returns start line" {
        val expectedLine = 8
        val listItemCache = TestListItemCache(
            -1,
            TestPos(TestLoc(expectedLine, 0, 0), TestLoc(expectedLine, 0, 0))
        )

        val actualLine = cacheItemLine(listItemCache)
        actualLine shouldBe expectedLine
    }

    "lineContents returns string version of the ListItemCache" {
        val expectedFileContents = listOf(
            "- [ ] First Line",
            "- [ ] Second Line",
            "- [ ] Third Line"
        )
        val expectedLineContents = "[ ] Second Line"
        val expectedLine = 1
        val listItemCache = TestListItemCache(
            -1,
            TestPos(TestLoc(expectedLine, 0, 0), TestLoc(expectedLine, 0, 0))
        )

        val actualLineContents = lineContents(expectedFileContents, listItemCache)
        actualLineContents shouldBe expectedLineContents
    }
})