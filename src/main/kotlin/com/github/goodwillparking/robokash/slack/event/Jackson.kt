package com.github.goodwillparking.robokash.slack.event

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule



object EventSerializer {
    val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun deserialize(json: String) = objectMapper.readValue(json, Event::class.java)
}
