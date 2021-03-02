package com.github.goodwillparking.robokash.slack.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.goodwillparking.robokash.slack.ChannelId
import com.github.goodwillparking.robokash.slack.UserId

// TODO: don't respond to these events as it can result in double responses
private const val MENTION_TYPE = "app_mention"

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = UnknownInner::class,
    visible = true
)
@JsonSubTypes(
    Type(ChatMessage::class, name = MENTION_TYPE),
    Type(ChatMessage::class, name = "message")
)
sealed class InnerEvent

/**
 * @property user The sender.
 */
data class ChatMessage(
    val text: String,
    val user: UserId,
    val channel: ChannelId,
    val isMention: Boolean
) : InnerEvent() {

    // https://api.slack.com/events/app_mention
    @JsonCreator
    private constructor(
        text: String,
        user: UserId,
        channel: ChannelId,
        type: String
    ): this(text, user, channel, type == MENTION_TYPE)
}

data class UnknownInner(val type: String) : InnerEvent()

