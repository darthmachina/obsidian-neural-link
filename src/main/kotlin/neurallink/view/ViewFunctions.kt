package neurallink.view

import io.kvision.html.Span
import neurallink.core.service.BOLD_REGEX
import neurallink.core.service.ITALIC_REGEX
import neurallink.core.service.WIKILINK_REGEX


/**
 * Creates a new string converting Italics and Bold to HTML
 */
fun markdownToStyle(text: String) : String {
    return markdownItalicToStyle(markdownBoldToStyle(markdownLinkToStyle(text)))
}

fun markdownBoldToStyle(text: String) : String {
    return text.replace(BOLD_REGEX, "<span class=\"nl-bold\">$1</span>")
}

fun markdownItalicToStyle(text: String) : String {
    return text.replace(ITALIC_REGEX, "<span class=\"nl-italic\">\$1</span>")
}

fun markdownLinkToStyle(text: String) : String {
    return text.replace(WIKILINK_REGEX, "<span class=\"nl-wikilink\">\$1</span>")
}

/**
 * Parses out wiki-style markdown links.
 *
 * @return a tokenized list with all links starting with !
 */
fun parseMarkdownLinks(text: String) : List<String> {
    return text.replace(WIKILINK_REGEX, "|!\$1|").split("|")
}
