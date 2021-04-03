package com.github.goodwillparking.robokash.dao.sqlite.message

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Timestamp
import java.time.Instant


data class Message(
    val text: String,
    val timestamp: Timestamp,
    val channel: String,
    @JsonProperty("client_msg_id")
    val clientMessageId: String?,
    val user: String
)
