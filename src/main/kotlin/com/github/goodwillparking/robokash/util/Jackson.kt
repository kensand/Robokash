package com.github.goodwillparking.robokash.util

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.goodwillparking.robokash.slack.BoxedValue
import com.github.goodwillparking.robokash.slack.ChannelId
import com.github.goodwillparking.robokash.slack.UserId

object DefaultSerializer {
    val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(SlackModule)
        .registerModule(JavaTimeModule())
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun serialize(any: Any): String = objectMapper.writeValueAsString(any)

    inline fun <reified T> deserialize(json: String): T = objectMapper.readValue(json, T::class.java)
}

private object SlackModule : SimpleModule(
    "SlackModule",
    Version.unknownVersion(),
    mapOf(
        UserId::class.java to UserIdDeserializer,
        ChannelId::class.java to ChannelIdDeserializer,
    ),
    listOf(
        ToStringSerializer(BoxedValue::class.java)
    )
)

private abstract class BoxedStringDeserializer<S : Any>(clazz: Class<S>) : FromStringDeserializer<S>(clazz) {
    override fun _deserialize(value: String, ctxt: DeserializationContext): S = mapString(value)

    abstract fun mapString(value: String): S
}

private object UserIdDeserializer : BoxedStringDeserializer<UserId>(UserId::class.java) {
    override fun mapString(value: String) = UserId(value)
}

private object ChannelIdDeserializer : BoxedStringDeserializer<ChannelId>(ChannelId::class.java) {
    override fun mapString(value: String) = ChannelId(value)
}
