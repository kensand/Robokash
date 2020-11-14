package com.github.goodwillparking.robokash.keyword

data class Keyword(
    val value: String,
    val probability: Double,
    val ignoreCase: Boolean = true,
    val exactMatch: Boolean = true
) {

    fun matches(word: String): Boolean {
        // TODO: support non-exact matches
        return word.equals(value, ignoreCase)
    }
}

class Keywords private constructor(private val map: Map<String, Keyword>): Map<String, Keyword> by map {

    constructor(keywords: Iterable<Keyword>) : this(keywords.associateBy { it.value })

//    operator fun contains(keyword: Keyword) = contains(keyword.value)
}
