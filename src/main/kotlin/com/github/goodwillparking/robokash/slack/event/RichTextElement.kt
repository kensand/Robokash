package com.github.goodwillparking.robokash.slack.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = UnknownRichTextElement::class,
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(RichTextSection::class, name = "rich_text_section")
)
sealed class RichTextElement

data class RichTextSection(val elements: List<RichTextSectionElement>) : RichTextElement() {

    val mentions by lazy { elements.filterIsInstance<RichTextSectionElement.User>().map { it.user_id } }
}

data class UnknownRichTextElement(val type: String) : RichTextElement()
