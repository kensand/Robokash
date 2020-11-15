package com.github.goodwillparking.robokash.slack.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = Unknown::class,
    visible = true
)
@JsonSubTypes(
    Type(AppMention::class, name = "app_mention"),
    Type(Message::class, name = "message")
)
sealed class Event

interface ChatMessage {
    val text: String
}

data class AppMention(override val text: String) : Event(), ChatMessage

data class Message(override val text: String) : Event(), ChatMessage

data class Unknown(val type: String) : Event()

