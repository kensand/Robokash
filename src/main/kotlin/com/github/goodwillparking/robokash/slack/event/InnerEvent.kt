package com.github.goodwillparking.robokash.slack.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = UnknownInner::class,
    visible = true
)
@JsonSubTypes(
    Type(AppMention::class, name = "app_mention"),
    Type(Message::class, name = "message")
)
sealed class InnerEvent

interface ChatMessage {
    val text: String
}

data class AppMention(override val text: String) : InnerEvent(), ChatMessage

data class Message(override val text: String) : InnerEvent(), ChatMessage

data class UnknownInner(val type: String) : InnerEvent()

