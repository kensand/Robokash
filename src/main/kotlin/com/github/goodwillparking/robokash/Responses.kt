package com.github.goodwillparking.robokash

data class Responses(val values: List<String>) {
    constructor(response: String): this(listOf(response))
}
