package com.github.goodwillparking.robokash.slack

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.github.goodwillparking.robokash.slack.SlackEventHandler.Companion.AUTH_VERSION
import com.github.goodwillparking.robokash.slack.SlackEventHandler.Companion.NO_RETRY_HEADER
import com.github.goodwillparking.robokash.slack.SlackEventHandler.Companion.RETRY_COUNT_HEADER
import com.github.goodwillparking.robokash.slack.SlackEventHandler.Companion.SIGNATURE_HEADER
import com.github.goodwillparking.robokash.slack.SlackEventHandler.Companion.TIMESTAMP_HEADER
import com.github.goodwillparking.robokash.slack.event.ChatMessage
import com.github.goodwillparking.robokash.slack.event.Event
import com.github.goodwillparking.robokash.slack.event.EventWrapper
import com.github.goodwillparking.robokash.slack.event.UrlVerification
import com.github.goodwillparking.robokash.util.DefaultSerializer.serialize
import com.github.goodwillparking.robokash.util.ResourceUtil.loadTextResource
import com.github.goodwillparking.robokash.util.Try.Success
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
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

    "bot should deny requests with invalid signatures" {
        with(testHandler()) {
            val apiGatewayRequest = createRequest(chatMessageMention, overrideSignature = "invalid")
            handle(
                request = apiGatewayRequest,
                expectedStatusCode = 403,
                expectedHeaders = mapOf(
                    NO_RETRY_HEADER to "1" // Can be any arbitrary value.
                )
            )
        }
    }

    "bot should respond to url verification requests" {
        with(testHandler()) {
            val urlVerification = UrlVerification("It's time to d-d-d-d, d-d-d-d-duel!")
            val apiGatewayRequest = createRequest(urlVerification)
            handle(
                request = apiGatewayRequest,
                expectedStatusCode = 200,
                expectedBody = urlVerification.challenge
            )
        }
    }

    "bot should not respond to retries and request no more retries" {
        with(testHandler()) {
            val apiGatewayRequest = createRequest(chatMessageMention)
                .addHeaders(RETRY_COUNT_HEADER to "1")
            handle(
                request = apiGatewayRequest,
                expectedHeaders = mapOf(NO_RETRY_HEADER to "1")
            )
        }
    }

    "bot should throw" - {
        "on unknown events" {
            with(testHandler()) {
                shouldThrow<IllegalArgumentException> {
                    handle(createRequest(loadTextResource("/slack/events/app-requested.json")))
                }
            }
        }

        "on unknown inner events" {
            with(testHandler()) {
                shouldThrow<IllegalArgumentException> {
                    handle(createRequest(loadTextResource("/slack/events/reaction-added.json")))
                }
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

private fun SlackEventHandler.handle(
    request: APIGatewayProxyRequestEvent,
    expectedStatusCode: Int = 200,
    expectedHeaders: Map<String, String>? = null,
    expectedBody: String? = null
) = handleRequest(request, mockk()).apply {
    statusCode shouldBe expectedStatusCode
    headers shouldBe expectedHeaders
    body shouldBe expectedBody
}

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

private fun SlackEventHandler.createRequest(
    event: Event,
    overrideSignature: String? = null
) = createRequest(serialize(event), overrideSignature)

private fun SlackEventHandler.createRequest(
    body: String,
    overrideSignature: String? = null
): APIGatewayProxyRequestEvent {
    val requestTime = Instant.now()

    val signature = overrideSignature ?: Auth.produceSignature(
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

private fun APIGatewayProxyRequestEvent.addHeaders(vararg headers: Pair<String, String>) =
    withHeaders(this.headers + headers.asIterable())

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
