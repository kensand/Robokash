package com.github.goodwillparking.robokash.slack.event

import io.kotest.assertions.asClue
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.types.beInstanceOf
import kotlin.reflect.KClass

internal class EventSerializationTest : FreeSpec({

    "events should deserialize" - {
        forAll<EventSetup>(
            "url-verification" to { EventSetup(it, UrlVerification::class) },
            "wrapper" to EventSetup("message", EventWrapper::class),
            "unknown" to EventSetup("app-requested", Unknown::class)
        ) { (fileName, eventType) -> deserializeFromFile(fileName, eventType) }
    }

    "inner events should deserialize" - {
        forAll<EventSetup>(
            "mention" to { EventSetup(it, AppMention::class) },
            "message" to { EventSetup(it, Message::class) },
            "unknown inner" to EventSetup("reaction-added", UnknownInner::class)
        ) { (fileName, innerType) ->
            deserializeFromFile<EventWrapper>(fileName) { deserialized ->
                deserialized.event should beInstanceOf(innerType)
            }
        }
    }
})

private infix fun <A, B> A.to(mapper: (A) -> B): Pair<A, B> = Pair(this, mapper(this))

private data class EventSetup(val jsonFile: String, val expectedEventType: KClass<*>)

private inline fun <reified T : Any> Any.deserializeFromFile(fileName: String, noinline block: (T) -> Unit) =
    deserializeFromFile(fileName, T::class, block)

private fun <T : Any> Any.deserializeFromFile(fileName: String, type: KClass<T>, block: (T) -> Unit = { }) {
    loadTextResource("/slack/events/$fileName.json").asClue { json ->
        EventSerializer.deserialize(json).asClue { deserialized ->
            deserialized should beInstanceOf(type)
            block(deserialized as T)
        }
    }
}

private fun Any.loadTextResource(path: String) = this::class.java.getResource(path)?.readText()
    ?: throw IllegalArgumentException("No resource found at path $path")
