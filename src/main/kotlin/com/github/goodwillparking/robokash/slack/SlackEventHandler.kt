package com.github.goodwillparking.robokash.slack

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.github.goodwillparking.robokash.RandomResponseCheck
import com.github.goodwillparking.robokash.ResponseCheck
import com.github.goodwillparking.robokash.UserSpecificResponseCache
import com.github.goodwillparking.robokash.slack.event.Event
import com.github.goodwillparking.robokash.slack.event.EventCallback
import com.github.goodwillparking.robokash.slack.event.Message
import com.github.goodwillparking.robokash.slack.event.UnknownEvent
import com.github.goodwillparking.robokash.slack.event.UnknownInnerEvent
import com.github.goodwillparking.robokash.slack.event.UrlVerification
import com.github.goodwillparking.robokash.util.DefaultSerializer.deserialize
import com.github.goodwillparking.robokash.util.ResourceUtil.loadTextResource
import mu.KotlinLogging
import java.time.Instant
import kotlin.math.min
import kotlin.random.Random

private val log = KotlinLogging.logger { }

/**
 * Handler for requests to Lambda function.
 */
class SlackEventHandler(
    val props: BotInstanceProperties = BotInstanceProperties(
        accessToken = System.getenv("BOT_ACCESS_TOKEN"),
        signingSecret = System.getenv("BOT_SIGNING_SECRET"),
        userId = UserId(System.getenv("BOT_USER_ID")),
        responseProbabilityConfig = ResponseProbabilityConfig(
            // TODO: find a better way to configure this
            responseProbability = System.getenv("RESPONSE_CHANCE").toDouble(),
            maxReplyProbability = System.getenv("MAX_REPLY_PROBABILITY").toDouble(),
            maxMentionReplyProbability = System.getenv("MAX_MENTION_REPLY_PROBABILITY").toDouble(),
            maxReplyProbabilityThreshold = System.getenv("MAX_REPLY_PROBABILITY_THRESHOLD").toInt()
        )
    ),
    val random: Random = Random,
    val slackInterface: SlackInterface = LiveSlackInterface(
        botAccessToken = props.accessToken
    ),
    responseProvider: () -> Responses = DEFAULT_RESPONSE_PROVIDER,
    responseConfigProvider: () -> UserSpecificResponseCache.Config = DEFAULT_RESPONSE_CONFIG_PROVIDER
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
            deserialize(loadTextResource("/responses.json"))
        }

        private val DEFAULT_RESPONSE_CONFIG_PROVIDER: () -> UserSpecificResponseCache.Config = {
            deserialize(loadTextResource("/user-specific-responses.json"))
        }
    }

    private val responses: Responses by lazy(responseProvider)

    val userSpecificResponseConfig by lazy(responseConfigProvider)

    private var userSpecificResponseCache = UserSpecificResponseCache(responses, userSpecificResponseConfig)

    override fun handleRequest(
        request: APIGatewayProxyRequestEvent,
        context: Context
    ): APIGatewayProxyResponseEvent = try {

        log.debug { "Request: $request" }

        when {
            !isRequesterValid(request) -> createInvalidRequesterResponse()
            RETRY_COUNT_HEADER in request.headers -> createRetryResponse()
            else -> parseEvent(request)
        }
    } catch (e: Exception) {
        log.error(e) { "Failed to process request: $request" }
        throw e
    }

    private fun createRetryResponse(): APIGatewayProxyResponseEvent {
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

    private fun isRequesterValid(request: APIGatewayProxyRequestEvent): Boolean {
        val timestamp = requireNotNull(request.headers[TIMESTAMP_HEADER]) { "Missing timestamp header" }
            .let(String::toLong)
            .let(Instant::ofEpochSecond)
        val requestSig = requireNotNull(request.headers[SIGNATURE_HEADER]) { "Missing signature header" }

        val sig = Auth.produceSignature(
            key = props.signingSecret,
            body = request.body,
            timestamp = timestamp,
            version = AUTH_VERSION
        )

        val signatureMatches = requestSig.equals(sig, ignoreCase = true)
        if (!signatureMatches) log.warn { "Unauthorized request: $sig, request: $request" }
        return signatureMatches
    }

    private fun createInvalidRequesterResponse(): APIGatewayProxyResponseEvent = APIGatewayProxyResponseEvent()
        // In case this really was Slack and we've got a bug in our auth code, ask Slack to stop retrying.
        .withHeaders(mapOf(NO_RETRY_HEADER to "1"))
        .withStatusCode(403)

    private fun parseEvent(request: APIGatewayProxyRequestEvent) = when (val event = deserialize<Event>(request.body)) {
        is EventCallback<*> -> {
            when (val inner = event.event) {
                is Message -> createChatMessageResponse(inner)
                is UnknownInnerEvent -> throw UnknownSlackInnerEventException(inner)
            }
        }
        is UrlVerification -> createUrlVerificationResponse(event)
        is UnknownEvent -> throw UnknownSlackEventException(event)
    }

    private fun createUrlVerificationResponse(verification: UrlVerification): APIGatewayProxyResponseEvent {
        log.info { "URL verification request" }
        return APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody(verification.challenge)
    }

    private fun createChatMessageResponse(message: Message): APIGatewayProxyResponseEvent {
        if (message.user == props.userId) {
            log.info { "The event was triggered by the bot. Ignore it." }
        } else {
            val replyCheck = getPotentialReplies(message)
                ?.let { RandomResponseCheck(it, getReplyProbability(message, it)) }
            val globalCheck = RandomResponseCheck(responses, getGlobalProbability(message))
            listOf(replyCheck, globalCheck).asSequence()
                .map { it?.let(::role) }
                .filterNotNull()
                .firstOrNull()
                ?.also { respond(it, message) }
        }
        return APIGatewayProxyResponseEvent().withStatusCode(200)
    }

    private fun getPotentialReplies(message: Message): Responses? {
        val (updated, responses) = userSpecificResponseCache[message.user]
        userSpecificResponseCache = updated
        return responses
    }

    private fun getReplyProbability(message: Message, replyResponses: Responses): Double =
        with(props.responseProbabilityConfig) {
            val base = if (message.mentionsBot()) maxMentionReplyProbability else maxReplyProbability
            val multiplier = min(replyResponses.size.toDouble() / maxReplyProbabilityThreshold, 1.0)
            base * multiplier
        }

    private fun getGlobalProbability(message: Message): Double =
        with(props.responseProbabilityConfig) { if (message.mentionsBot()) 1.0 else responseProbability }

    private fun role(check: ResponseCheck): String? {
        val result = role(random, check.probability)
        log.debug { result }
        return result.takeIf { it.isSuccess }?.let { check.getResponse() }
    }

    private fun role(random: Random, probability: Double): RoleResult {
        val rolled = random.nextDouble(0.0, 1.0)
        val required = 1.0 - probability
        val success = when (probability) {
            // Check for 0 in case the Random rolls a 1.0 exactly.
            0.0 -> false
            else -> rolled + probability >= 1.0
        }

        return RoleResult(
            required = required,
            actual = rolled,
            isSuccess = success
        )
    }

    private fun respond(response: String, message: Message) {
        log.info { "Posting message $response" }
        val result = slackInterface.postMessage(response, message.channel).getOrThrow()
        log.debug { "Post message result: $result" }
    }

    private data class RoleResult(val required: Double, val actual: Double, val isSuccess: Boolean)

    private fun Message.mentionsBot() = props.userId in mentions
}
