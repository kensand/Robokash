package com.github.goodwillparking.robokash.util

fun String.trimWhitespace() = filter { !it.isWhitespace() }
