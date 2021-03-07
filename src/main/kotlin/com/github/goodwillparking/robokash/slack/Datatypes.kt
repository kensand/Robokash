package com.github.goodwillparking.robokash.slack

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
