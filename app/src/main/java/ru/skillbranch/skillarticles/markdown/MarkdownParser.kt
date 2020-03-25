package ru.skillbranch.skillarticles.markdown

import java.util.regex.Pattern

// объект, в котором мы будем разбирать наш язык разметки Markdown
object MarkdownParser {

    // private val LINE_SEPARATOR = System.getProperty("line.separator") ?: "/n"
    private val LINE_SEPARATOR = "\n"

    // group regex
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-] .+$)"
    private const val HEADER_GROUP =
        "(^#{1,6} .+?$)" // ? делает квантифатор нежадным (тот берёт как можно меньше символов)
    private const val QUOTE_GROUP = "(^> .+?$)"
    private const val ITALIC_GROUP = "((?<!\\*)\\*[^*].*?\\*(?!\\*)|(?<!_)_[^_].*?_(?!_))"
    private const val BOLD_GROUP =
        "((?<!\\*)\\*{2}[^*].*?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?_{2}(?!_))"
    private const val STRIKE_GROUP = "((?<!~)~{2}[^~].*?~{2}(?!~))"
    private const val RULE_GROUP = "(^[*]{3}|[-]{3}|[_]{3}$)"
    private const val INLINE_GROUP = "((?<!`)`[^`\\s].*?`(?!`))"
    private const val LINK_GROUP = "(\\[[^\\[\\]]*?\\]\\(.+?\\)|^\\[.*?]\\(.*?\\))"
    private const val BLOCK_CODE_GROUP = "(^```[^\\r]+?```\$)"
    private const val ORDER_LIST_GROUP = "(^\\d\\. .+$)"

    // result regex
    private const val MARKDOWN_GROUPS =
        "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP|$ITALIC_GROUP|$BOLD_GROUP" +
            "|$STRIKE_GROUP|$RULE_GROUP|$INLINE_GROUP|$LINK_GROUP|$BLOCK_CODE_GROUP|$ORDER_LIST_GROUP"

    private val elementsPattern by lazy { Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE) }

    /**
     * parse markdown text to elements
     */
    fun parse(string: String): MarkdownText {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return MarkdownText(elements)
    }

    private fun List<Element>.toClearString(): String {
        return buildString {
            this@toClearString.forEach { el ->
                if (el.elements.isEmpty()) append(el.text)
                else append(el.elements.toClearString())
            }
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

    /**
     * find markdown elements in markdown text
     * метод из markdown строки парсит элементы и возвращает их
     */
    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)
        // индекс, с которого матчер будет постоянно искать следующее вхождение
        var lastStartIndex = 0

        loop@ while (matcher.find(lastStartIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            // if something is found then everything before - TEXT
            if (lastStartIndex < startIndex) {
                parents.add(Element.Text(string.subSequence(lastStartIndex, startIndex)))
            }

            // found text
            var text: CharSequence

            // groups range for iterate by groups (1..9) or (1..11) optionally
            val groups = 1..11

            var group = -1
            for (gr in groups) {
                // 01:01:30
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
                    val reg = "^#{1,6}".toRegex().find(
                        string.subSequence(
                            startIndex,
                            endIndex
                        )
                    ) // регулярное выражение для нахождения #
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

                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.Italic(text, subs)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // BOLD
                5 -> {
                    // text without "**{}**"
                    text = string.subSequence(startIndex.plus(2), endIndex.plus(-2))

                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.Bold(text, subs)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // STRIKE
                6 -> {
                    // text without "~~{}~~"
                    text = string.subSequence(startIndex.plus(2), endIndex.plus(-2))

                    // find inner elements
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
                    // деструктурирование по группам реглярного выражения
                    val (title: String, link: String) = "\\[(.*)]\\((.*)\\)".toRegex().find(text)!!.destructured

                    val element = Element.Link(link, title)
                    parents.add(element)

                    lastStartIndex = endIndex
                }
                // 10 -> BLOCK CODE - optionally
                10 -> {
                    // text without "```{}```"
                    text = string.subSequence(startIndex.plus(3), endIndex.plus(-3))

                    val lines = text.split("\n")
                    if (lines.size == 1) {
                        val element = Element.BlockCode(Element.BlockCode.Type.SINGLE, lines.first())
                        parents.add(element)
                    }
                    else {
                        for (ind in lines.indices) {
                            val line = lines[ind]
                            val element = when (ind) {
                                0 -> Element.BlockCode(Element.BlockCode.Type.START, line + LINE_SEPARATOR)
                                lines.lastIndex -> Element.BlockCode(Element.BlockCode.Type.END, line)
                                else -> Element.BlockCode(Element.BlockCode.Type.MIDDLE, line + LINE_SEPARATOR)
                            }
                            parents.add(element)
                        }
                    }

                    lastStartIndex = endIndex
                }

                // 11 -> NUMERIC LIST
                11 -> {
                    val order = string.subSequence(startIndex, startIndex + 2).toString()

                    // текст без маркера списка
                    text = string.subSequence(startIndex.plus(3), endIndex)

                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.OrderedListItem(order, text, subs)
                    parents.add(element)

                    // next find start from position "end index" (last regex character)
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
        val type: Type = Type.MIDDLE,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element() {
        enum class Type { START, END, MIDDLE, SINGLE }
    }
}