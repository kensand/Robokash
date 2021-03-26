package com.github.goodwillparking.robokash.slack

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.github.goodwillparking.robokash.slack.SlackEventHandler.Companion.NO_RETRY_HEADER
import com.github.goodwillparking.robokash.slack.SlackEventHandler.Companion.RETRY_COUNT_HEADER
import com.github.goodwillparking.robokash.slack.SlackEventHandlerTestUtils.CHAT_MESSAGE
import com.github.goodwillparking.robokash.slack.SlackEventHandlerTestUtils.CHAT_MESSAGE_MENTION
import com.github.goodwillparking.robokash.slack.SlackEventHandlerTestUtils.testHandler
import com.github.goodwillparking.robokash.slack.event.UrlVerification
import com.github.goodwillparking.robokash.util.ResourceUtil.loadTextResource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec

internal class SlackEventHandlerTest : FreeSpec({

    "bot should respond" - {
        "when it is mentioned" {
            with(testHandler()) {
                expectSuccessfulPost()
                expectRoll()

                handle(createRequest(CHAT_MESSAGE_MENTION))
                verifySuccessfulPost()
            }
        }

        "when it rolls successfully" {
            with(testHandler(probability = 0.1)) {
                expectSuccessfulPost()
                expectRoll(0.9)

                handle(createRequest(CHAT_MESSAGE))
                verifySuccessfulPost()
            }
        }

        "when probability is 1.0" {
            with(testHandler(probability = 1.0)) {
                expectSuccessfulPost()
                expectRoll(0.0)

                handle(createRequest(CHAT_MESSAGE))
                verifySuccessfulPost()
            }
        }
    }

    "bot should not respond" - {
        "when it rolls unsuccessfully" {
            with(testHandler(probability = 0.1)) {
                expectRoll(0.899999)
                handle(createRequest(CHAT_MESSAGE))
            }
        }

        "when it the probability is 0" {
            with(testHandler(probability = 0.0)) {
                expectRoll(1.0)
                handle(createRequest(CHAT_MESSAGE))
            }
        }

        "when the message is from the bot" {
            with(testHandler(probability = 1.0)) {
                val apiGatewayRequest = createRequest(CHAT_MESSAGE.innerCopy { copy(user = props.userId) })
                expectRoll(1.0)
                handle(apiGatewayRequest)
            }
        }
    }

    "bot should deny requests with invalid signatures" {
        with(testHandler()) {
            val apiGatewayRequest = createRequest(CHAT_MESSAGE_MENTION, overrideSignature = "invalid")
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
            val apiGatewayRequest = createRequest(CHAT_MESSAGE_MENTION)
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
                shouldThrow<UnknownSlackEventException> {
                    handle(createRequest(loadTextResource("/slack/events/app-requested.json")))
                }
            }
        }

        "on unknown inner events" {
            with(testHandler()) {
                shouldThrow<UnknownSlackInnerEventException> {
                    handle(createRequest(loadTextResource("/slack/events/reaction-added.json")))
                }
            }
        }
    }
})

private fun APIGatewayProxyRequestEvent.addHeaders(vararg headers: Pair<String, String>) =
    withHeaders(this.headers + headers.asIterable())
