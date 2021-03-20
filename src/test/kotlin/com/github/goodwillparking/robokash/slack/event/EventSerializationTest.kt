package com.github.goodwillparking.robokash.slack.event

import com.github.goodwillparking.robokash.slack.UserId
import com.github.goodwillparking.robokash.util.DefaultSerializer
import com.github.goodwillparking.robokash.util.ResourceUtil.loadTextResource
import io.kotest.assertions.asClue
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.reflect.KClass

internal class EventSerializationTest : FreeSpec({

    "events should deserialize" - {
        forAll<EventSetup<*>>(
            "url-verification" to { EventSetup(it, UrlVerification::class) },
            "event-callback" to EventSetup("message", EventCallback::class),
            "unknown" to EventSetup("app-requested", UnknownEvent::class)
        ) { (fileName, eventType) -> deserializeFromFile(fileName, eventType) }
    }

    "inner events should deserialize" - {
        "message" {
            deserializeFromFile<EventCallback<*>>("message") { deserialized ->
                deserialized.event.apply {
                    shouldBeInstanceOf<Message>()
                    text shouldBe "<@ABCDEFG> a b c <!channel>"
                    mentions shouldContain UserId("ABCDEFG")
                }
            }

        }
        "unknown" {
            deserializeFromFile<EventCallback<*>>("reaction-added") { it.event.shouldBeInstanceOf<UnknownInnerEvent>() }
        }
    }
})

private infix fun <A, B> A.to(mapper: (A) -> B): Pair<A, B> = Pair(this, mapper(this))

private data class EventSetup<T : Any>(val jsonFile: String, val expectedEventType: KClass<T>)

private inline fun <reified T : Any> Any.deserializeFromFile(fileName: String, noinline block: (T) -> Unit) =
    deserializeFromFile(fileName, T::class, block)

private fun <T : Any> deserializeFromFile(fileName: String, type: KClass<T>, block: (T) -> Unit = { }) {
    loadTextResource("/slack/events/$fileName.json").asClue { json ->
        DefaultSerializer.deserialize<Event>(json).asClue { deserialized ->
            deserialized should beInstanceOf(type)
            block(deserialized as T)
        }
    }
}
