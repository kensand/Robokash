package com.github.goodwillparking.robokash.util

fun String.trimWhitespace() = filter { !it.isWhitespace() }

object ResourceUtil {
    fun loadTextResource(path: String) = this::class.java.getResource(path)?.readText()
        ?: throw IllegalArgumentException("No resource found at path $path")
}
