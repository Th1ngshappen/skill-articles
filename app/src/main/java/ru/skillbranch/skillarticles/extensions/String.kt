package ru.skillbranch.skillarticles.extensions

fun String?.indexesOf(substr: String, ignoreCase: Boolean = true): List<Int> {

    if (this.isNullOrEmpty() || substr.isEmpty()) return emptyList()

    val regex =  if (ignoreCase) Regex(substr, RegexOption.IGNORE_CASE) else Regex(substr)

    return regex.findAll(this).map { it.range.first }.toList()
}