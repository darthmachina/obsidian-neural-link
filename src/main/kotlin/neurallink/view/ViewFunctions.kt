package neurallink.view

import io.kvision.core.Color
import neurallink.core.model.Tag
import neurallink.core.service.BOLD_REGEX
import neurallink.core.service.ITALIC_REGEX
import neurallink.core.service.WIKILINK_REGEX

val DEFAULT_AREA_COLOR = Color.hex(0x5a5a5a)

fun findTagColor(cardTags: Set<Tag>, tagColors: Map<Tag, String>) : Color {
    cardTags.forEach { tag ->
        if (tag in tagColors.keys) {
            return Color("#" + tagColors[tag]!!)
        }
    }
    return DEFAULT_AREA_COLOR
}

// ********* Markdown Functions *********
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
