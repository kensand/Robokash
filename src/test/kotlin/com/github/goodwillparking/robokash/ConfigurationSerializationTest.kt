package com.github.goodwillparking.robokash

import com.github.goodwillparking.robokash.slack.Responses
import com.github.goodwillparking.robokash.util.DefaultSerializer
import com.github.goodwillparking.robokash.util.DefaultSerializer.deserialize
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainInOrder

internal class ConfigurationSerializationTest : FreeSpec({

    "responses should deserialize" {
        val json = """
            |{
            |  "values": [
            |    "foo",
            |    "bar"
            |  ]
            |}
        """.trimMargin()
        val responses = deserialize<Responses>(json)
        responses.values shouldContainInOrder listOf("foo", "bar")
    }
})
