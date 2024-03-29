package neurallink.view

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kvision.core.Color
import neurallink.core.model.Tag
import neurallink.test.TestFactory

@Suppress("unused")
class ViewFunctionsTest : StringSpec({
    "findTagColor finds the right color for a tag" {
        val tags = setOf(Tag("test"))
        val tagColors = mapOf(Pair(Tag("test"), "ffffff"))

        val expectedColor = Color("#${tagColors[Tag("test")]}")
        val actualColor = findTagColor(tags, tagColors)

        actualColor.color shouldBe expectedColor.color
    }

    "findTagColor returns the default Color if not tag exists" {
        val tags = setOf(Tag("foo"))
        val tagColors = mapOf(Pair(Tag("test"), "ffffff"))

        val expectedColor = Color("#5a5a5a")
        val actualColor = findTagColor(tags, tagColors)

        actualColor.color shouldBe expectedColor.color
    }

    // markdownBoldToStyle()
    "markdownBoldToStyle works on regular text" {
        val testText = "Test **bold** text"
        val expectedText = "Test <span class=\"nl-bold\">bold</span> text"

        val actualText = markdownBoldToStyle(testText)
        actualText shouldBe expectedText
    }

    // markdownItalicToStyle()
    "markdownItalicToStyle works on regular text" {
        val testText = "Test *italic* text"
        val expectedText = "Test <span class=\"nl-italic\">italic</span> text"

        val actualText = markdownItalicToStyle(testText)
        actualText shouldBe expectedText
    }

    // markdownLinkToStyle()
    "markdownLinkToStyle works on regular text" {
        val testText = "Test [[link]] text"
        val expectedText = "Test <span class=\"nl-wikilink\">link</span> text"

        val actualText = markdownLinkToStyle(testText)
        actualText shouldBe expectedText
    }

    // markdownToStyle()
    "markdownToStyle works with just bold" {
        val testText = "Test **bold** text"
        val expectedText = "Test <span class=\"nl-bold\">bold</span> text"

        val actualText = markdownToStyle(testText)
        actualText shouldBe expectedText
    }

    "markdownToStyle works with just italics" {
        val testText = "Test *italic* text"
        val expectedText = "Test <span class=\"nl-italic\">italic</span> text"

        val actualText = markdownToStyle(testText)
        actualText shouldBe expectedText
    }

    "markdownToStyle works with just a link" {
        val testText = "Test [[link]] text"
        val expectedText = "Test <span class=\"nl-wikilink\">link</span> text"

        val actualText = markdownToStyle(testText)
        actualText shouldBe expectedText
    }

    "markdownToStyle works with separate bold and italic" {
        val testText = "Test **bold** and *italic* text"
        val expectedText = "Test <span class=\"nl-bold\">bold</span> and <span class=\"nl-italic\">italic</span> text"

        val actualText = markdownToStyle(testText)
        actualText shouldBe expectedText
    }

    "markdownToStyle works with combined bold and italic" {
        val testText = "Test ***italic and bold*** text"
        val expectedText = "Test <span class=\"nl-italic\"><span class=\"nl-bold\">italic and bold</span></span> text"

        val actualText = markdownToStyle(testText)
        actualText shouldBe expectedText
    }

    "markdownToStyle works with bold and a link" {
        val testText = "Test [[link]] and **bold** text"
        val expectedText = "Test <span class=\"nl-wikilink\">link</span> and <span class=\"nl-bold\">bold</span> text"

        val actualText = markdownToStyle(testText)
        actualText shouldBe expectedText
    }

    "markdownToStyle works with italic and a link" {
        val testText = "Test [[link]] and *italic* text"
        val expectedText = "Test <span class=\"nl-wikilink\">link</span> and <span class=\"nl-italic\">italic</span> text"

        val actualText = markdownToStyle(testText)
        actualText shouldBe expectedText
    }
})
