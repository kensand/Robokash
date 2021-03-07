package com.github.goodwillparking.robokash.slack.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = Unknown::class,
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(EventWrapper::class, name = "event_callback"),
    JsonSubTypes.Type(UrlVerification::class, name = "url_verification")
)
sealed class Event

data class EventWrapper<I : InnerEvent>(val event: I, val event_id: String) : Event()

data class UrlVerification(val challenge: String) : Event()

data class Unknown(val type: String) : Event()
