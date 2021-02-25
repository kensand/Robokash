package com.github.goodwillparking.robokash.tools

import com.fasterxml.jackson.core.type.TypeReference
import com.github.goodwillparking.robokash.slack.event.EventSerializer.objectMapper
import com.github.goodwillparking.robokash.util.IoUtil
import java.io.File
import java.time.Instant

private val leadingQuoteRegex = Regex("^&gt;\\s*")
private val newlineQuoteRegex = Regex("\\n&gt;\\s*")
//private val chatQuoteRegex = Regex("\\[.*?].*?:\\s+") // Matches things like "[2/1/19, 4:25:28 PM] Jane Doe: "
private val timestampRegex = Regex("\\[.*\\d:\\d\\d.*]")
// TODO: add blacklist to exclude specific messages, eg: "yikes", "Andrey:", "Q:", "Akash:"

fun main(vararg args: String) {
    val outputDir = args[0]

    val json = IoUtil.loadTextResource("/raw-messages.json")
    val messages = objectMapper.readValue(json, object : TypeReference<List<LoggedMessage>>() {})
    val responses = Responses(messages.map {
        it.msg.replace(leadingQuoteRegex, "")
            .replace(newlineQuoteRegex, "\n")
//            .replace(chatQuoteRegex, "")
    }.filter { !it.contains(timestampRegex) })
//    println(responses.values.map { "\n\n$it" })

    println("${messages.size} source messages, ${responses.values.size} responses")

    val responsesJson = objectMapper.writeValueAsString(responses)
    File("$outputDir/responses.json").writeText(responsesJson)
}

data class Responses(val values: List<String>)

private data class LoggedMessage(val ts: Instant, val msg: String)
