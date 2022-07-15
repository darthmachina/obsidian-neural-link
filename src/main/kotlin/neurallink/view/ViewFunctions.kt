package neurallink.view

import io.kvision.panel.HPanel
import io.kvision.panel.hPanel
import neurallink.core.service.BOLD_REGEX
import neurallink.core.service.ITALIC_REGEX
import neurallink.core.service.WIKILINK_REGEX

fun markdownToStyle(text: String) : String {
    return markdownItalicToStyle(markdownBoldToStyle(text))
}

fun markdownBoldToStyle(text: String) : String {
    return text.replace(BOLD_REGEX, "<span class=\"nl-bold\">$1</span>")
}

fun markdownItalicToStyle(text: String) : String {
    return text.replace(ITALIC_REGEX, "<span class=\"nl-italic\">\$1</span>")
}

fun parseMarkdownLinks(text: String) : List<String> {
    return text.replace(WIKILINK_REGEX, "|!\$1|").split("|")
}
