package com.github.goodwillparking.robokash.slack.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = UnknownEvent::class,
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(EventCallback::class, name = "event_callback"),
    JsonSubTypes.Type(UrlVerification::class, name = "url_verification")
)
sealed class Event

data class EventCallback<I : InnerEvent>(val event: I, val event_id: String) : Event() {
    fun innerCopy(block: I.() -> I) = copy(event = event.block())
}

data class UrlVerification(val challenge: String) : Event()

data class UnknownEvent(val type: String) : Event()
