package com.github.goodwillparking.robokash.tools

import com.fasterxml.jackson.core.type.TypeReference
import com.github.goodwillparking.robokash.slack.Responses
import com.github.goodwillparking.robokash.util.DefaultSerializer.objectMapper
import com.github.goodwillparking.robokash.util.ResourceUtil
import java.io.File
import java.time.Instant

private val leadingQuoteRegex = Regex("^&gt;\\s*")
private val newlineQuoteRegex = Regex("\\n&gt;\\s*")
private val timestampRegex = Regex("\\[.*\\d:\\d\\d.*]")
// TODO: add blacklist to exclude specific messages, e.g. "Andrey:", "Q:", "Akash:"

fun main(vararg args: String) {
    val outputDir = args[0]

    val json = ResourceUtil.loadTextResource("/raw-messages.json")
    val messages = objectMapper.readValue(json, object : TypeReference<List<MessageWrapper>>() {})
        .map { it.message }
    val responses = Responses(messages.map {
        it.text.replace(leadingQuoteRegex, "")
            .replace(newlineQuoteRegex, "\n")
    }.filter { !it.contains(timestampRegex) })

    println("${messages.size} source messages, ${responses.values.size} responses")

    val responsesJson = objectMapper.writeValueAsString(responses)
    File("$outputDir/responses.json").writeText(responsesJson)
}

private data class MessageWrapper(val message: LoggedMessage, val timestamp: Instant)

private data class LoggedMessage(val text: String)
