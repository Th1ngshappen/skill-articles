package ru.skillbranch.skillarticles.data.repositories

import java.util.regex.Pattern
import java.lang.StringBuilder

// объект, в котором мы будем разбирать наш язык разметки Markdown
object MarkdownParser {

    // private val LINE_SEPARATOR = System.getProperty("line.separator") ?: "/n"
    private val LINE_SEPARATOR = "\n"

    // group regex
    // ? делает квантифатор нежадным (тот берёт как можно меньше символов)
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-] .+$)"
    private const val HEADER_GROUP = "(^#{1,6} .+?$)"
    private const val QUOTE_GROUP = "(^> .+?$)"
    private const val ITALIC_GROUP = "((?<!\\*)\\*[^*].*?\\*(?!\\*)|(?<!_)_[^_].*?_(?!_))"
    private const val BOLD_GROUP = "((?<!\\*)\\*{2}[^*].*?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?_{2}(?!_))"
    private const val STRIKE_GROUP = "((?<!~)~{2}[^~].*?~{2}(?!~))"
    private const val RULE_GROUP = "(^[*]{3}|[-]{3}|[_]{3}$)"
    private const val INLINE_GROUP = "((?<!`)`[^`\\s].*?`(?!`))"
    private const val LINK_GROUP = "(\\[[^\\[\\]]*?\\]\\(.+?\\)|^\\[.*?]\\(.*?\\))"
    private const val BLOCK_CODE_GROUP = "(^```[\\s\\S]+?```\$)"
    private const val ORDER_LIST_GROUP = "(^\\d{1,2}\\. .+$)"
    private const val IMAGE_GROUP = "(^!\\[[^\\[\\]]*?\\]\\(.*?\\)\$)"

    // result regex
    private const val MARKDOWN_GROUPS =
        "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP|$ITALIC_GROUP|$BOLD_GROUP|$STRIKE_GROUP" +
                "|$RULE_GROUP|$INLINE_GROUP|$LINK_GROUP|$BLOCK_CODE_GROUP|$ORDER_LIST_GROUP|$IMAGE_GROUP"

    private val elementsPattern by lazy { Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE) }

    /**
     * parse markdown text to elements
     */
    fun parse(string: String): List<MarkdownElement> {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return elements.fold(mutableListOf()) { acc, element ->
            val last = acc.lastOrNull()
            when (element) {
                is Element.Image -> acc.add(
                    MarkdownElement.Image(
                        element,
                        last?.bounds?.second ?: 0
                    )
                )
                is Element.BlockCode -> acc.add(
                    MarkdownElement.Scroll(
                        element,
                        last?.bounds?.second ?: 0
                    )
                )
                else -> {
                    if (last is MarkdownElement.Text) last.elements.add(element)
                    else acc.add(
                        MarkdownElement.Text(
                            mutableListOf(element),
                            last?.bounds?.second ?: 0
                        )
                    )
                }
            }
            acc
        }
    }

    /**
     * clear markdown text to string without markdown characters
     * возвращает строку, очищенную от маркдаунов (нужно для дальнейшего поиска в ней)
     */
    fun clear(string: String?): String? {
        string ?: return null
        return findElements(string).toClearString()
    }

    private fun List<Element>.toClearString(): String {
        return buildString {
            this@toClearString.forEach { el ->
                if (el.elements.isEmpty()) {
                    append(el.text)
                } else append(el.elements.toClearString())
            }
        }
    }

    /**
     * find markdown elements in markdown text
     * метод из markdown строки парсит элементы и возвращает их
     */
    private fun findElements(string: CharSequence): List<Element> {

        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)

        // индекс, с которого матчер будет каждую итерацию искать следующее вхождение
        var lastStartIndex = 0

        loop@ while (matcher.find(lastStartIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            // if something is found then everything before - TEXT
            if (lastStartIndex < startIndex) {
                parents.add(
                    Element.Text(
                        string.subSequence(lastStartIndex, startIndex)
                    )
                )
            }

            // found text
            var text: CharSequence

            // groups range for iterate by groups
            val groups = 1..12

            var group = -1
            for (gr in groups) {
                // 5: 01:01:30
                if (matcher.group(gr) != null) {
                    group = gr
                    break
                }
            }

            when (group) {
                // NOT FOUND -> BREAK
                -1 -> break@loop

                // UNORDERED LIST
                1 -> {
                    // текст без маркера списка
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.UnorderedListItem(text, subs)
                    parents.add(element)

                    // next find start from position "end index" (last regex character)
                    lastStartIndex = endIndex
                }

                // HEADER
                2 -> {
                    // регулярное выражение для нахождения #
                    val reg = "^#{1,6}".toRegex().find(
                        string.subSequence(startIndex, endIndex)
                    )
                    val level = reg!!.value.length

                    // text without "{#} "
                    text = string.subSequence(startIndex.plus(level.inc()), endIndex)

                    val element = Element.Header(level, text)
                    parents.add(element)
                    lastStartIndex = endIndex

                }

                // QUOTE
                3 -> {
                    // text without "> "
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.Quote(text, subs)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // ITALIC
                4 -> {
                    // text without "*{}*"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())

                    val subs = findElements(text)
                    val element = Element.Italic(text, subs)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // BOLD
                5 -> {
                    // text without "**{}**"
                    text = string.subSequence(startIndex.plus(2), endIndex.plus(-2))

                    val subs = findElements(text)
                    val element = Element.Bold(text, subs)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // STRIKE
                6 -> {
                    // text without "~~{}~~"
                    text = string.subSequence(startIndex.plus(2), endIndex.plus(-2))

                    val subs = findElements(text)
                    val element = Element.Strike(text, subs)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // RULE
                7 -> {
                    // text without "***" insert empty character
                    // к этому пустому символу впоследствии сможем прикрепить спан (спану нужен хотя бы один символ) 01:21:20
                    val element = Element.Rule()
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // INLINE CODE
                8 -> {
                    // text without "`{}`"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())

                    val element = Element.InlineCode(text)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // LINK
                9 -> {
                    // full text for regex
                    text = string.subSequence(startIndex, endIndex)
                    // деструктурирование по группам регулярного выражения
                    val (title: String, link: String) = "\\[(.*)]\\((.*)\\)".toRegex().find(text)!!.destructured

                    val element = Element.Link(link, title)
                    parents.add(element)

                    lastStartIndex = endIndex
                }
                // 10 -> BLOCK CODE - optionally
                10 -> {
                    // text without "```{}```"
                    text = string.subSequence(startIndex.plus(3), endIndex.plus(-3))
                    val element = Element.BlockCode(text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // 11 -> NUMERIC LIST
                11 -> {
                    val reg =
                        "^\\d{1,2}\\.".toRegex().find(string.subSequence(startIndex, endIndex))
                    val order = reg!!.value

                    text = string.subSequence(startIndex.plus(order.length.inc()), endIndex)

                    val subs = findElements(text)
                    val element = Element.OrderedListItem(order, text, subs)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // 12 -> IMAGE GROUP
                12 -> {
                    text = string.subSequence(startIndex, endIndex)
                    val (alt, url, title) = "^!\\[([^\\[\\]]*?)?]\\((.*?) \"(.*?)\"\\)$".toRegex()
                        .find(text)!!.destructured

                    val element = Element.Image(url, alt, title)
                    parents.add(element)

                    lastStartIndex = endIndex
                }
            }
        }

        if (lastStartIndex < string.length) {
            val text = string.subSequence(lastStartIndex, string.length)
            parents.add(Element.Text(text))
        }

        return parents
    }

}

data class MarkdownText(val elements: List<Element>)

sealed class MarkdownElement() {
    abstract val offset: Int // 6: 2:09:33 общее количество символов предшествующих markdown элементов
    // границы текста, который относится к данному markdown элементу that is bound to custom view group
    val bounds: Pair<Int, Int> by lazy {
        val end = when (this) {
            is Text -> {
                 elements.fold(offset) { acc, el ->
                    acc + el.spread().map { it.text.length }.sum()
                }
            }
            is Image -> image.text.length + offset
            is Scroll -> blockCode.text.length + offset
        }
        // начальная граница равна индексу символа, следующего за индексом предшеств. символа (offset)
        (if (offset == 0) 0 else offset.inc()) to end
    }

    data class Text(
        val elements: MutableList<Element>,
        override val offset: Int = 0
    ) : MarkdownElement()

    data class Image(
        val image: Element.Image,
        override val offset: Int = 0
    ) : MarkdownElement()

    data class Scroll(
        val blockCode: Element.BlockCode,
        override val offset: Int = 0
    ) : MarkdownElement()
}

// соответствует элементу разметки
// каждый markdown элемент может содержать в себе дочерние подэлементы
sealed class Element() {
    abstract val text: CharSequence
    abstract val elements: List<Element>

    data class Text(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class UnorderedListItem(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Header(
        val level: Int = 1,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Quote(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Italic(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Bold(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Strike(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Rule(
        override val text: CharSequence = " ", // for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence, // for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link: String,
        override val text: CharSequence, // for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class OrderedListItem(
        val order: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Image(
        val url: String,
        val alt: String?,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()
}

private fun Element.spread(): List<Element> {
    val elements = mutableListOf<Element>()
    if (this.elements.isNotEmpty()) elements.addAll(
        this.elements.spread()
    ) else elements.add(this)
    return elements
}

private fun List<Element>.spread(): List<Element> {
    val elements = mutableListOf<Element>()
    forEach {
        elements.addAll(it.spread())
    }
    return elements
}

private fun Element.clearContent(): String {
    return StringBuilder().apply {
        val element = this@clearContent
        if (element.elements.isEmpty()) append(element.text)
        else element.elements.forEach {
            append(it.clearContent())
        }
    }.toString()
}

fun List<MarkdownElement>.clearContent(): String {
    return StringBuilder().apply {
        this@clearContent.forEach {
            when (it) {
                is MarkdownElement.Text -> it.elements.forEach { el ->
                    append(el.clearContent())
                }
                is MarkdownElement.Image -> append(it.image.clearContent())
                is MarkdownElement.Scroll -> append(it.blockCode.clearContent())
            }
        }
    }.toString()
}