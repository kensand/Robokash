package com.github.goodwillparking.robokash.dao.sqlite.message

import com.github.goodwillparking.robokash.util.ResourceUtil
import mu.KotlinLogging
import org.sqlite.SQLiteConfig
import java.io.File
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashSet

val log = KotlinLogging.logger { }

class MessageDao(dbPath: String = ResourceUtil.loadResourcePath("/db.sqlite").toAbsolutePath().toString()) {
    val connection: Connection

    init {

        log.info(
            "Opening database at $dbPath, running in ${File(".").absolutePath}, contains ${
                Files.walk(File(".").toPath()).map { path -> path.toUri().toASCIIString() }.collect(Collectors.toList())
                    .joinToString { x -> x }
            }"
        )

        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
    }

    fun insertMessages(messages: List<Message>): Int {
        val preparedStatement =
            connection.prepareStatement("INSERT INTO messages (client_message_id, timestamp, channel, user, text) VALUES (?, ?, ?, ?, ?) ON CONFLICT (text) DO NOTHING ")
        messages.forEach {
            preparedStatement.setString(1, it.clientMessageId)
            preparedStatement.setTimestamp(2, it.timestamp)
            preparedStatement.setString(3, it.channel)
            preparedStatement.setString(4, it.user)
            preparedStatement.setString(5, it.text)
            preparedStatement.addBatch()
        }

        return preparedStatement.executeBatch().toList().sum()
    }

    fun getMessages(): Set<Message> {
        val resultSet = connection.createStatement()
            .executeQuery("SELECT client_message_id, timestamp, channel, user, text from messages")
        val mutableMessages: MutableSet<Message> = HashSet()
        while (resultSet.next()) {
            mutableMessages.add(
                Message(
                    text = resultSet.getString(5),
                    user = resultSet.getString(4),
                    channel = resultSet.getString(3),
                    timestamp = resultSet.getTimestamp(2),
                    clientMessageId = resultSet.getString(1)
                )
            )
        }
        return mutableMessages.toHashSet()
    }
}