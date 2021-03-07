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
import java.time.Instant

internal class SlackEventHandlerTest : FreeSpec({

    "bot should respond" - {
        "when it is mentioned" {
            val handler = SlackEventHandler(
                props = BotInstanceProperties(
                    accessToken = "accessToken",
                    signingSecret = "signingSecret",
                    userId = UserId("botUser")
                ),
                slackInterface = mockk(),
                random = mockk(),
                responseProvider = { Responses("testResponse") },
                responseProbability = 0.0
            )

            val event = EventWrapper(
                ChatMessage(
                    text = "Dude!",
                    user = UserId("triggerUser"),
                    channel = ChannelId("channel"),
                    isMention = true
                ),
                "slackEventId"
            )

            val body = serialize(event)

            val requestTime = Instant.now()
            val signature = Auth.produceSignature(
                key = handler.props.signingSecret,
                body = body,
                requestTime,
                AUTH_VERSION
            )

            val apiGatewayRequest = APIGatewayProxyRequestEvent()
                .withBody(body)
                .withHeaders(mapOf(
                    TIMESTAMP_HEADER to requestTime.epochSecond.toString(),
                    SIGNATURE_HEADER to signature
                ))

            every { handler.slackInterface.postMessage(any(), event.event.channel) } returns Success("TADA!")

            handler.handleRequest(apiGatewayRequest, mockk())
        }
    }
})
