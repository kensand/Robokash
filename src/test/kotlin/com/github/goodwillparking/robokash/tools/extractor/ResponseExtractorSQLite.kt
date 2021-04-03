package com.github.goodwillparking.robokash.tools

import com.fasterxml.jackson.core.type.TypeReference
import com.github.goodwillparking.robokash.dao.sqlite.message.Message
import com.github.goodwillparking.robokash.dao.sqlite.message.MessageDao
import com.github.goodwillparking.robokash.tools.extractor.LoggedMessage
import com.github.goodwillparking.robokash.tools.extractor.MessageWrapper
import com.github.goodwillparking.robokash.util.DefaultSerializer.objectMapper
import com.github.goodwillparking.robokash.util.ResourceUtil
import com.github.goodwillparking.robokash.util.ResourceUtil.loadResourcePath
import com.github.goodwillparking.robokash.util.ResourceUtil.loadTextResource
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.sql.Timestamp

private val leadingQuoteRegex = Regex("^&gt;\\s*")
private val newlineQuoteRegex = Regex("\\n&gt;\\s*")
// TODO: Support "attachments"
// TODO: Join with separate message file

fun main(vararg args: String) {
    val parsedArgs = ArgParser(args).parseInto(::Args)
    val json = loadTextResource("/raw-messages.json")
    val messages = objectMapper.readValue(json, object : TypeReference<List<MessageWrapper>>() {})
        .map { it to objectMapper.readValue(it.message, object : TypeReference<LoggedMessage>() {}) }
        .map {
            Message(
                it.second.text,
                Timestamp.from(it.first.timestamp),
                it.first.channel,
                it.second.clientMsgId,
                it.second.user
            )
        }

    val messageDao = MessageDao(parsedArgs.dbFile)

    val insertedCount = messageDao.insertMessages(messages)

    println("${messages.size} source messages, inserted $insertedCount")
}

class Args(parser: ArgParser) {
    val dbFile: String by parser.storing("-d", "--dbFile", help = "Relative path to the SQLite Database file")
        .default(loadResourcePath("/db.sqlite").toAbsolutePath().toString())
}