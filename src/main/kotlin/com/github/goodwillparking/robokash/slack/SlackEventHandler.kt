package com.github.goodwillparking.robokash.slack

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.github.goodwillparking.robokash.Responses
import com.github.goodwillparking.robokash.slack.event.ChatMessage
import com.github.goodwillparking.robokash.slack.event.DefaultSerializer
import com.github.goodwillparking.robokash.slack.event.DefaultSerializer.deserialize
import com.github.goodwillparking.robokash.slack.event.Event
import com.github.goodwillparking.robokash.slack.event.EventWrapper
import com.github.goodwillparking.robokash.slack.event.Unknown
import com.github.goodwillparking.robokash.slack.event.UnknownInner
import com.github.goodwillparking.robokash.slack.event.UrlVerification
import com.github.goodwillparking.robokash.util.ResourceUtil
import mu.KotlinLogging
import java.time.Instant
import kotlin.random.Random

private val log = KotlinLogging.logger { }

/**
 * Handler for requests to Lambda function.
 */
class SlackEventHandler(
    val props: BotInstanceProperties = BotInstanceProperties(
        accessToken = System.getenv("BOT_ACCESS_TOKEN"),
        signingSecret = System.getenv("BOT_SIGNING_SECRET"),
        userId = UserId(System.getenv("BOT_USER_ID"))
    ),
    val random: Random = Random.Default,
    val slackInterface: SlackInterface = LiveSlackInterface(
        botAccessToken = props.accessToken
    ),
    val responseProbability: Double = System.getenv("RESPONSE_CHANCE").toDouble(),
    responseProvider: () -> Responses = DEFAULT_RESPONSE_PROVIDER
) : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    companion object {
        // It is always v0
        // https://api.slack.com/authentication/verifying-requests-from-slack#verifying-requests-from-slack-using-signing-secrets__a-recipe-for-security__how-to-make-a-request-signature-in-4-easy-steps-an-overview
        internal const val AUTH_VERSION = "v0"
        internal const val NO_RETRY_HEADER = "X-Slack-No-Retry"
        internal const val RETRY_COUNT_HEADER = "X-Slack-Retry-Num"
        internal const val SIGNATURE_HEADER = "X-Slack-Signature"
        internal const val TIMESTAMP_HEADER = "X-Slack-Request-Timestamp"

        private val DEFAULT_RESPONSE_PROVIDER: () -> Responses = {
            val text = ResourceUtil.loadTextResource("/responses.json")
            DefaultSerializer.objectMapper.readValue(text, Responses::class.java)
        }
    }

    private val responses: Responses by lazy(responseProvider)

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {

        log.debug { "INPUT: $input" }

        verifyCaller(input)?.also { return it }

        if (RETRY_COUNT_HEADER in input.headers) {
            // Slack will retry up to 3 times (4 total attempts).
            // Robokash can be a bit slow, so he might timeout and trigger some retries.
            // Ignore the retries because Robokash probably got the initial request
            // and is just taking his sweet time to respond.
            // https://api.slack.com/events-api#the-events-api__field-guide__error-handling__graceful-retries
            log.info { "This is a retry from Slack, ignore it." }
            return APIGatewayProxyResponseEvent()
                // Ask Slack to pls stahp
                // https://api.slack.com/events-api#the-events-api__field-guide__error-handling__graceful-retries__turning-retries-off
                .withHeaders(mapOf(NO_RETRY_HEADER to "1"))
                .withStatusCode(200)
        }

        return when (val event = deserialize<Event>(input.body)) {
            is EventWrapper<*> -> {
                when (val inner = event.event) {
                    is ChatMessage -> {
                        if (inner.user == props.userId) {
                            log.info { "The event was triggered by the bot. Ignore it." }
                        } else {
                            determineResponse(inner)?.also { respond(it, inner) }
                        }
                        APIGatewayProxyResponseEvent().withStatusCode(200)
                    }
                    is UnknownInner -> throw IllegalArgumentException("Received event with unknown inner event: $event")
                }
            }
            is UrlVerification -> createUrlVerification(event)
            is Unknown -> throw IllegalArgumentException("Received unknown event: $event")
        }
    }

    private fun verifyCaller(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent? {
        val timestamp = requireNotNull(input.headers[TIMESTAMP_HEADER]) { "Missing timestamp header" }
            .let(String::toLong)
            .let(Instant::ofEpochSecond)
        val requestSig = requireNotNull(input.headers[SIGNATURE_HEADER]) { "Missing signature header" }
        
        val sig = Auth.produceSignature(
            key = props.signingSecret,
            body = input.body,
            timestamp = timestamp,
            version = AUTH_VERSION
        )

        return if (!requestSig.equals(sig, ignoreCase = true)) {
            log.warn { "Unauthorized request: $sig" }
            APIGatewayProxyResponseEvent()
                // In case this really was Slack and we've got a bug in our auth code, ask Slack to stop retrying.
                .withHeaders(mapOf(NO_RETRY_HEADER to "1"))
                .withStatusCode(403)
        } else null
    }

    private fun createUrlVerification(verification: UrlVerification): APIGatewayProxyResponseEvent {
        log.info { "URL verification request" }
        return APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody(verification.challenge)
    }

    private fun determineResponse(chatMessage: ChatMessage): String? = when {
        responses.values.isEmpty() -> {
            log.info { "No responses defined." }
            null
        }
        chatMessage.isMention || role(random, responseProbability) -> responses.values.random()
        else -> null
    }

    // TODO: RoleResult(val required: Double, val actual: Double, val isSuccess: Boolean)
    private fun role(random: Random, probability: Double): Boolean =
        when (probability) {
            0.0 -> false // Check for 0 in case the Random rolls a 1.0 exactly.
            else -> random.nextDouble(0.0, 1.0) + probability >= 1.0
        }

    private fun respond(response: String, message: ChatMessage) {
        val result = slackInterface.postMessage(response, message.channel).getOrThrow()
        log.debug { "Post message result: $result" }
    }
}
