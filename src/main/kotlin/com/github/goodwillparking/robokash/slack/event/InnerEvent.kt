package com.github.goodwillparking.robokash.slack.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.goodwillparking.robokash.slack.ChannelId
import com.github.goodwillparking.robokash.slack.UserId

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = UnknownInnerEvent::class,
    visible = true
)
@JsonSubTypes(
    Type(Message::class, name = "message")
)
sealed class InnerEvent

/**
 * @property user The sender.
 */
data class Message(
    val text: String,
    val user: UserId,
    val channel: ChannelId,
    val blocks: List<Block> = emptyList()
) : InnerEvent() {

    val mentions by lazy { blocks.filterIsInstance<RichText>().flatMap { it.mentions } }
}

data class UnknownInnerEvent(val type: String) : InnerEvent()
