package com.github.goodwillparking.robokash.util

object IoUtil {
    fun loadTextResource(path: String) = this::class.java.getResource(path)?.readText()
        ?: throw IllegalArgumentException("No resource found at path $path")
}
