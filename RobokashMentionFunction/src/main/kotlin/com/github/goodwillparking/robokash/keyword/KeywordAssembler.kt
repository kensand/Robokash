package com.github.goodwillparking.robokash.keyword

object KeywordAssembler {

    private val wordRegex = Regex("""\W""")

    fun findKeywords(message: String, keywords: Iterable<Keyword>): KeywordResponse {
        val words = message.getWords()
        // TODO: Don't match case
        val matchingKeywords = keywords.filter { words.contains(it.value) }
        return KeywordResponse(message, Keywords(matchingKeywords))
    }

    // TODO: This currently can return whitespace
    private fun String.getWords() = split(wordRegex)

    data class KeywordResponse(val contents: String, val keywords: Keywords)
}
