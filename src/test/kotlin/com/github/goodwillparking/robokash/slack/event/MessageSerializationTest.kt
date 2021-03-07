package com.github.goodwillparking.robokash.slack.event

import com.github.goodwillparking.robokash.slack.ChannelId
import com.github.goodwillparking.robokash.slack.PostMessage
import com.github.goodwillparking.robokash.slack.event.DefaultSerializer.serialize
import com.github.goodwillparking.robokash.util.ResourceUtil.loadTextResource
import com.github.goodwillparking.robokash.util.trimWhitespace
import io.kotest.assertions.asClue
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.reflect.KClass

internal class MessageSerializationTest : FreeSpec({

    "PostMessage should serialize" {
        val json = serialize(PostMessage(ChannelId("myChannel"), "hi"))
        println(ChannelId("myChannel"))
        json shouldBe """
            {
                "channel": "myChannel",
                "text": "hi"
            }
        """.trimWhitespace()
    }
})
