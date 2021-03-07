package com.github.goodwillparking.robokash.slack.event

import com.github.goodwillparking.robokash.slack.ChannelId
import com.github.goodwillparking.robokash.slack.PostMessage
import com.github.goodwillparking.robokash.util.DefaultSerializer.serialize
import com.github.goodwillparking.robokash.util.trimWhitespace
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

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
