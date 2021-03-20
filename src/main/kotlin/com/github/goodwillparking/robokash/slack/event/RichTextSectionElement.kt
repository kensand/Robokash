package com.github.goodwillparking.robokash.slack.event

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.goodwillparking.robokash.slack.UserId
import com.github.goodwillparking.robokash.slack.event.RichTextSectionElement.Broadcast
import com.github.goodwillparking.robokash.slack.event.RichTextSectionElement.Text
import com.github.goodwillparking.robokash.slack.event.RichTextSectionElement.Unknown
import com.github.goodwillparking.robokash.slack.event.RichTextSectionElement.User

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = Unknown::class,
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(User::class, name = "user"),
    JsonSubTypes.Type(Text::class, name = "text"),
    JsonSubTypes.Type(Broadcast::class, name = "broadcast")
)
sealed class RichTextSectionElement {

    data class User(val user_id: UserId) : RichTextSectionElement()

    data class Text(val text: String) : RichTextSectionElement()

    data class Broadcast(val range: Range) : RichTextSectionElement() {

        enum class Range {
            channel,
            @JsonEnumDefaultValue
            unknown
        }
    }

    data class Unknown(val type: String) : RichTextSectionElement()
}
