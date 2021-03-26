package com.github.goodwillparking.robokash.slack

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.github.goodwillparking.robokash.UserSpecificResponseCache
import com.github.goodwillparking.robokash.slack.event.Event
import com.github.goodwillparking.robokash.slack.event.EventCallback
import com.github.goodwillparking.robokash.slack.event.Message
import com.github.goodwillparking.robokash.slack.event.RichText
import com.github.goodwillparking.robokash.slack.event.RichTextSection
import com.github.goodwillparking.robokash.slack.event.RichTextSectionElement
import com.github.goodwillparking.robokash.util.DefaultSerializer
import com.github.goodwillparking.robokash.util.Try
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

object SlackEventHandlerTestUtils {

    val BOT_ID = UserId("botUser")

    val SENDER = UserId("triggerUser")

    val CHANNEL_ID = ChannelId("channel")

    val CHAT_MESSAGE = EventCallback(createMessage(), "slackEventId")

    val CHAT_MESSAGE_MENTION = EventCallback(
        createMessage(mentions = listOf(BOT_ID)),
        "slackEventId"
    )

    fun testHandler(
        probability: Double = 0.0,
        maxReplyProbability: Double = 0.0,
        maxMentionReplyProbability: Double = 0.0,
        maxReplyProbabilityThreshold: Int = 1,
        responses: Responses = Responses("testResponse"),
        replyConfig: List<UserSpecificResponseCache.UserConfig> = emptyList()
    ) = SlackEventHandler(
        props = BotInstanceProperties(
            accessToken = "accessToken",
            signingSecret = "signingSecret",
            userId = BOT_ID,
            responseProbabilityConfig = ResponseProbabilityConfig(
                responseProbability = probability,
                maxReplyProbability = maxReplyProbability,
                maxMentionReplyProbability = maxMentionReplyProbability,
                maxReplyProbabilityThreshold = maxReplyProbabilityThreshold
            )
        ),
        slackInterface = mockk(),
        random = mockk(),
        responseProvider = { responses },
        responseConfigProvider = { UserSpecificResponseCache.Config(replyConfig) }
    )

    fun createMessage(
        baseText: String = "Dude! ",
        user: UserId = SENDER,
        channel: ChannelId = CHANNEL_ID,
        mentions: List<UserId> = emptyList()
    ): Message {
        val text = baseText + mentions.joinToString(separator = " ", prefix = "<@", postfix = ">")

        val elements = listOf<RichTextSectionElement>(RichTextSectionElement.Text(baseText)) +
            mentions.map { RichTextSectionElement.User(it) }

        return Message(
            text = text,
            user = user,
            channel = channel,
            blocks = listOf(RichText(listOf(RichTextSection(elements))))
        )
    }
}

fun SlackEventHandler.expectRoll(result: Double = 0.0) {
    require(result >= 0)
    require(result <= 1.0)
    every { random.nextDouble(0.0, 1.0) } returns result
}

fun SlackEventHandler.expectSuccessfulPost(
    message: String? = null,
    channel: ChannelId = SlackEventHandlerTestUtils.CHANNEL_ID
) {
    every { slackInterface.postMessage(message ?: any(), channel) } returns Try.Success("It is done.")
}

fun SlackEventHandler.verifySuccessfulPost(
    message: String? = null,
    channel: ChannelId = SlackEventHandlerTestUtils.CHANNEL_ID,
    times: Int = 1
) {
    verify(exactly = times) { slackInterface.postMessage(message ?: any(), channel) }
}

fun SlackEventHandler.handle(
    request: APIGatewayProxyRequestEvent,
    expectedStatusCode: Int = 200,
    expectedHeaders: Map<String, String>? = null,
    expectedBody: String? = null
) = handleRequest(request, mockk()).apply {
    statusCode shouldBe expectedStatusCode
    headers shouldBe expectedHeaders
    body shouldBe expectedBody
}

fun SlackEventHandler.createRequest(
    event: Event,
    overrideSignature: String? = null
) = createRequest(DefaultSerializer.serialize(event), overrideSignature)

fun SlackEventHandler.createRequest(
    body: String,
    overrideSignature: String? = null
): APIGatewayProxyRequestEvent {
    val requestTime = Instant.now()

    val signature = overrideSignature ?: Auth.produceSignature(
        key = props.signingSecret,
        body = body,
        requestTime,
        SlackEventHandler.AUTH_VERSION
    )

    return APIGatewayProxyRequestEvent()
        .withBody(body)
        .withHeaders(mapOf(
            SlackEventHandler.TIMESTAMP_HEADER to requestTime.epochSecond.toString(),
            SlackEventHandler.SIGNATURE_HEADER to signature
        ))
}
