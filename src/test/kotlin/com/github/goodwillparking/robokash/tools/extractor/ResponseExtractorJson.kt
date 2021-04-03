package com.github.goodwillparking.robokash.tools

import com.fasterxml.jackson.core.type.TypeReference
import com.github.goodwillparking.robokash.slack.Responses
import com.github.goodwillparking.robokash.tools.extractor.LoggedMessage
import com.github.goodwillparking.robokash.tools.extractor.MessageFilter
import com.github.goodwillparking.robokash.tools.extractor.MessageWrapper
import com.github.goodwillparking.robokash.util.DefaultSerializer.objectMapper
import com.github.goodwillparking.robokash.util.ResourceUtil.loadTextResource
import java.io.File
import java.time.Instant

private val leadingQuoteRegex = Regex("^&gt;\\s*")
private val newlineQuoteRegex = Regex("\\n&gt;\\s*")
// TODO: Support "attachments"
// TODO: Join with separate message file

fun main(vararg args: String) {
    val outputDir = args[0]

    val json = loadTextResource("/raw-messages.json")
    val messages = objectMapper.readValue(json, object : TypeReference<List<MessageWrapper>>() {})
        .map { objectMapper.readValue(it.message, object : TypeReference<LoggedMessage>() {}) }

    val filter = objectMapper.readValue(
        loadTextResource("/message-filter.json"),
        MessageFilter::class.java
    )

    val responses = Responses(messages.asSequence()
        .map {
            it.text.replace(leadingQuoteRegex, "")
                .replace(newlineQuoteRegex, "\n")
        }
        .filter { it !in filter.blockedMessages }
        .filter { msg -> !filter.regexps.any { it in msg } }
        .toList())

    println("${messages.size} source messages, ${responses.values.size} responses")

    val responsesJson = objectMapper.writeValueAsString(responses)
    File("$outputDir/responses.json").writeText(responsesJson)
}
