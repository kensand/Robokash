package com.github.goodwillparking.robokash.slack.event

import io.kotest.assertions.asClue
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.beInstanceOf
import kotlin.reflect.KClass

internal class EventSerializationTest : FreeSpec({

    "event deserialization tests" - {
        forAll<EventSetup>(
            "mention" to { EventSetup(it, AppMention::class) },
            "message" to { EventSetup(it, Message::class) },
            "unknown" to EventSetup("reaction-added", Unknown::class)
        ) { (fileName, eventType) ->
            loadTextResource("/slack/events/$fileName.json").asClue { json ->
                eventMapper.readValue(json, EventWrapper::class.java).asClue { deserialized ->
                    deserialized.event should beInstanceOf(eventType)
                }
            }
        }
    }
})

private infix fun <A, B> A.to(mapper: (A) -> B): Pair<A, B> = Pair(this, mapper(this))

private data class EventSetup(val jsonFile: String, val expectedEventType: KClass<*>)

private fun Any.loadTextResource(path: String) = this::class.java.getResource(path)?.readText()
    ?: throw IllegalArgumentException("No resource found at path $path")
