package neurallink.view

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("unused")
class ViewFunctionsTest : StringSpec({
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
})
