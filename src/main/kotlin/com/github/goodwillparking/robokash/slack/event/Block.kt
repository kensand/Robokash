package com.github.goodwillparking.robokash.slack.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * See [block-kit](https://api.slack.com/block-kit),
 * [block-elements](https://api.slack.com/reference/block-kit/block-elements)
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = UnknownBlock::class,
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(RichText::class, name = "rich_text")
)
sealed class Block

data class RichText(val elements: List<RichTextElement>) : Block() {

    val mentions by lazy { elements.filterIsInstance<RichTextSection>().flatMap { it.mentions } }
}

data class UnknownBlock(val type: String) : Block()


