package com.github.goodwillparking.robokash.slack

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.github.goodwillparking.robokash.Responses
import com.github.goodwillparking.robokash.slack.SlackEventHandler.Companion.AUTH_VERSION
import com.github.goodwillparking.robokash.slack.SlackEventHandler.Companion.SIGNATURE_HEADER
import com.github.goodwillparking.robokash.slack.SlackEventHandler.Companion.TIMESTAMP_HEADER
import com.github.goodwillparking.robokash.slack.Try.Success
import com.github.goodwillparking.robokash.slack.event.ChatMessage
import com.github.goodwillparking.robokash.slack.event.DefaultSerializer.serialize
import com.github.goodwillparking.robokash.slack.event.EventWrapper
import io.kotest.core.spec.style.FreeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

internal class SlackEventHandlerTest : FreeSpec({

    "bot should respond" - {
        "when it is mentioned" {
            with(testHandler()) {
                expectSuccessfulPost()

                handle(createRequest(chatMessageMention))
                verifySuccessfulPost()
            }
        }

        "when it rolls successfully" {
            with(testHandler(probability = 0.1)) {
                expectSuccessfulPost()
                expectRoll(0.9)

                handle(createRequest(chatMessage))
                verifySuccessfulPost()
            }
        }

        "when probability is 1.0" {
            with(testHandler(probability = 1.0)) {
                expectSuccessfulPost()
                expectRoll(0.0)

                handle(createRequest(chatMessage))
                verifySuccessfulPost()
            }
        }
    }

    "bot should not respond" - {
        "when it rolls unsuccessfully" {
            with(testHandler(probability = 0.1)) {
                expectRoll(0.899999)
                handle(createRequest(chatMessage))
            }
        }

        "when it the probability is 0" {
            with(testHandler(probability = 0.0)) {
                expectRoll(1.0)
                handle(createRequest(chatMessage))
            }
        }

        "when the message is from the bot" {
            with(testHandler(probability = 1.0)) {
                val apiGatewayRequest = createRequest(chatMessage.innerCopy { copy(user = props.userId) })
                expectRoll(1.0)
                handle(apiGatewayRequest)
            }
        }
    }
})

private val channelId = ChannelId("channel")

private val chatMessage = EventWrapper(
    ChatMessage(
        text = "Dude!",
        user = UserId("triggerUser"),
        channel = channelId,
        isMention = false
    ),
    "slackEventId"
)

private val chatMessageMention = EventWrapper(
    ChatMessage(
        text = "Dude!",
        user = UserId("triggerUser"),
        channel = channelId,
        isMention = true
    ),
    "slackEventId"
)

private fun SlackEventHandler.handle(request: APIGatewayProxyRequestEvent) = handleRequest(request, mockk())

private fun SlackEventHandler.expectRoll(result: Double) {
    require(result >= 0)
    require(result <= 1.0)
    every { random.nextDouble(0.0, 1.0) } returns result
}

private fun SlackEventHandler.expectSuccessfulPost(channel: ChannelId = channelId) {
    every { slackInterface.postMessage(any(), channel) } returns Success("It is done.")
}

private fun SlackEventHandler.verifySuccessfulPost(channel: ChannelId = channelId) {
    verify(exactly = 1) { slackInterface.postMessage(any(), channel) }
}

private fun SlackEventHandler.createRequest(wrapper: EventWrapper<*>): APIGatewayProxyRequestEvent {
    val body = serialize(wrapper)
    val requestTime = Instant.now()

    val signature = Auth.produceSignature(
        key = props.signingSecret,
        body = body,
        requestTime,
        AUTH_VERSION
    )

    return APIGatewayProxyRequestEvent()
        .withBody(body)
        .withHeaders(mapOf(
            TIMESTAMP_HEADER to requestTime.epochSecond.toString(),
            SIGNATURE_HEADER to signature
        ))
}

private fun testHandler(probability: Double = 0.0) = SlackEventHandler(
    props = BotInstanceProperties(
        accessToken = "accessToken",
        signingSecret = "signingSecret",
        userId = UserId("botUser")
    ),
    slackInterface = mockk(),
    random = mockk(),
    responseProvider = { Responses("testResponse") },
    responseProbability = probability
)
