package com.github.goodwillparking.robokash.util

import java.nio.file.Path

fun String.trimWhitespace() = filter { !it.isWhitespace() }

object ResourceUtil {
    fun loadTextResource(path: String) = this::class.java.getResource(path)?.readText()
        ?: throw IllegalArgumentException("No resource found at path $path")

    fun loadResourcePath(path: String) = Path.of((this::class.java.getResource(path)
        ?: throw IllegalArgumentException("No resource found at path $path")).toURI())
}
