package com.github.goodwillparking.robokash.slack

import com.github.goodwillparking.robokash.UserSpecificResponseCache
import com.github.goodwillparking.robokash.slack.SlackEventHandlerTestUtils.CHAT_MESSAGE
import com.github.goodwillparking.robokash.slack.SlackEventHandlerTestUtils.CHAT_MESSAGE_MENTION
import com.github.goodwillparking.robokash.slack.SlackEventHandlerTestUtils.SENDER
import com.github.goodwillparking.robokash.slack.SlackEventHandlerTestUtils.testHandler
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.datatest.forAll

internal class DirectReplyTest : FreeSpec({

    "bot should reply" - {
        "when it rolls success" {
            with(testHandler(
                maxReplyProbability = 0.1,
                responses = responses,
                replyConfig = listOf(userConfig))
            ) {
                expectSuccessfulPost("pig")
                expectRoll(0.9)

                handle(createRequest(CHAT_MESSAGE))
                verifySuccessfulPost("pig")
            }
        }

        "when it rolls success and is mentioned" {
            with(testHandler(
                maxMentionReplyProbability = 0.1,
                responses = responses,
                replyConfig = listOf(userConfig))
            ) {
                expectSuccessfulPost("pig")
                expectRoll(0.9)

                handle(createRequest(CHAT_MESSAGE_MENTION))
                verifySuccessfulPost("pig")
            }
        }

        "when it rolls successes in succession" {
            with(testHandler(
                maxReplyProbability = 0.1,
                responses = responses,
                replyConfig = listOf(userConfig))
            ) {
                val iterations = 10
                expectSuccessfulPost("pig")
                expectRoll(0.9)
                repeat(iterations) { handle(createRequest(CHAT_MESSAGE)) }
                verifySuccessfulPost("pig", times = iterations)
            }
        }
    }

    "reply probability should be reduced if number of responses is below threshold" - {
        data class Setup(
            val roll: Double,
            val isMention: Boolean,
            val shouldReply: Boolean,
            val expectedReply: String? = null
        )

        forAll<Setup>(
            "above" to Setup(0.81, isMention = false, shouldReply = true, "pig"),
            "below" to Setup(0.79, isMention = false, shouldReply = false),
            "above mention" to Setup(0.81, isMention = true, shouldReply = true, "pig"),
            // Always replies with something on a mention
            "below mention" to Setup(0.79, isMention = true, shouldReply = true),
        ) { (roll, isMention, shouldReply, expectedReply) ->
            with(testHandler(
                maxReplyProbability = if (!isMention) 1.0 else 0.0,
                maxMentionReplyProbability = if (isMention) 1.0 else 0.0,
                // only one response, so probability should be reduced to 1/5 the original value
                maxReplyProbabilityThreshold = 5,
                responses = responses,
                replyConfig = listOf(userConfig))
            ) {
                if (shouldReply) expectSuccessfulPost(expectedReply)
                expectRoll(roll)

                handle(createRequest(if(isMention) CHAT_MESSAGE_MENTION else CHAT_MESSAGE))
                if (shouldReply) verifySuccessfulPost(expectedReply)
            }
        }
    }
})

private val responses = Responses((1..100000).map(Int::toString) + "pig")

private val userConfig = UserSpecificResponseCache.UserConfig(SENDER, listOf(Regex("pig")))
