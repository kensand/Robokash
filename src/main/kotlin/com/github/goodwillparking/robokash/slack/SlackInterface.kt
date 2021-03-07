package com.github.goodwillparking.robokash.slack

import com.github.goodwillparking.robokash.slack.event.DefaultSerializer
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

interface SlackInterface {
    fun postMessage(message: String, channelId: ChannelId): Try<String>
}

class LiveSlackInterface(val botAccessToken: String) : SlackInterface {

    companion object {
        private const val API_PATH = "https://slack.com/api/"
    }

    override fun postMessage(message: String, channelId: ChannelId): Try<String> = Try.of {
        // https://api.slack.com/methods/chat.postMessage
        val connection = URL(API_PATH + "chat.postMessage").openConnection() as HttpURLConnection

        val result = runCatching {
            connection.requestMethod = "POST"
            // https://api.slack.com/web#slack-web-api__basics__post-bodies__json-encoded-bodies
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $botAccessToken")
            connection.doOutput = true

            // Send request
            DataOutputStream(connection.outputStream).use {
                it.writeBytes(
                    DefaultSerializer.objectMapper.writeValueAsString(
                        PostMessage(channelId, message)
                    )
                )
            }

            // Get Response
            connection.inputStream.use { stream ->
                InputStreamReader(stream).useLines { it.joinToString(System.lineSeparator()) }
            }
        }

        connection.disconnect()
        result.getOrThrow()
    }
}
