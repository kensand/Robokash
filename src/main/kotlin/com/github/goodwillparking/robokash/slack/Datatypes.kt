package com.github.goodwillparking.robokash.slack

import com.github.goodwillparking.robokash.slack.event.UnknownEvent
import com.github.goodwillparking.robokash.slack.event.UnknownInnerEvent

abstract class BoxedValue<V>(val value: V) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoxedValue<*>

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int = value?.hashCode() ?: 0

    override fun toString(): String = value.toString()
}

abstract class BoxedString(value: String) : BoxedValue<String>(value)

class UserId(value: String) : BoxedString(value)

class ChannelId(value: String) : BoxedString(value)

data class PostMessage(val channel: ChannelId, val text: String)

data class BotInstanceProperties(val accessToken: String, val signingSecret: String, val userId: UserId)

data class Responses(val values: List<String>) {
    constructor(response: String): this(listOf(response))
}

data class UnknownSlackEventException(val unknown: UnknownEvent) :
    IllegalArgumentException("Unknown event type ${unknown.type}")

data class UnknownSlackInnerEventException(val unknown: UnknownInnerEvent) :
    IllegalArgumentException("Unknown inner event type ${unknown.type}")
