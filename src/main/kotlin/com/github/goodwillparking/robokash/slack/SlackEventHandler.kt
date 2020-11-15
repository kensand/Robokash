package com.github.goodwillparking.robokash.slack

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.goodwillparking.robokash.slack.event.EventSerializer
import com.github.goodwillparking.robokash.slack.event.EventWrapper
import com.github.goodwillparking.robokash.slack.event.Unknown
import com.github.goodwillparking.robokash.slack.event.UrlVerification
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handler for requests to Lambda function.
 */
class SlackEventHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    companion object {
        // It is always v0
        // https://api.slack.com/authentication/verifying-requests-from-slack#verifying-requests-from-slack-using-signing-secrets__a-recipe-for-security__how-to-make-a-request-signature-in-4-easy-steps-an-overview
        private const val authVersion = "v0"
    }

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {

        val log = context.logger

        log.log("INPUT: $input")

        verifyCaller(input, log)?.also { return it }

        if ("X-Slack-Retry-Num" in input.headers) {
            // Slack will retry up to 3 times (4 total attempts).
            // Robokash can be a bit slow, so he might timeout and trigger some retries.
            // Ignore the retries because Robokash probably got the initial request
            // and is just taking his sweet time to respond.
            // https://api.slack.com/events-api#the-events-api__field-guide__error-handling__graceful-retries
            log.log("This is a retry from Slack, ignore it.")
            return APIGatewayProxyResponseEvent()
                // Ask Slack to pls stahp
                // https://api.slack.com/events-api#the-events-api__field-guide__error-handling__graceful-retries__turning-retries-off
                .withHeaders(mapOf("X-Slack-No-Retry" to "1"))
                .withStatusCode(200)
        }

        return when (val event = EventSerializer.deserialize(input.body)) {
            is EventWrapper -> {
                // TODO: respond
                log.log("Got inner event: ${event.event}")
                APIGatewayProxyResponseEvent().withStatusCode(200)
            }
            is UrlVerification -> createUrlVerification(event, log)
            is Unknown -> throw IllegalArgumentException("Received unknown event: $event")
        }
    }

    private fun verifyCaller(input: APIGatewayProxyRequestEvent, log: LambdaLogger): APIGatewayProxyResponseEvent? {
        val timestamp = requireNotNull(input.headers["X-Slack-Request-Timestamp"]) { "Missing timestamp header" }
        val requestSig = requireNotNull(input.headers["X-Slack-Signature"]) { "Missing signature header" }
        
        val sig = Auth.produceSignature(
            key = System.getenv("BOT_SIGNING_SECRET"),
            body = input.body,
            timestamp = timestamp,
            version = authVersion
        )

        return if (!requestSig.equals(sig, ignoreCase = true)) {
            log.log("Unauthorized request: $sig")
            APIGatewayProxyResponseEvent()
                // In case this really was Slack and we've got a bug in our auth code, ask Slack to stop retrying.
                .withHeaders(mapOf("X-Slack-No-Retry" to "1"))
                .withStatusCode(403)
        } else null
    }

    private fun createUrlVerification(verification: UrlVerification, log: LambdaLogger): APIGatewayProxyResponseEvent {
        log.log("URL verification request")
        return APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody(verification.challenge)
    }

    private fun respond(request: JsonNode, log: LambdaLogger) {
        var connection: HttpURLConnection? = null

        try {
            // https://api.slack.com/methods/chat.postMessage
            val url = URL("https://slack.com/api/chat.postMessage")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            // https://api.slack.com/web#slack-web-api__basics__post-bodies__json-encoded-bodies
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${System.getenv("BOT_ACCESS_TOKEN")}")
            connection.doOutput = true

            val channel = request["event"]["channel"].textValue()

            // Send request
            DataOutputStream(connection.outputStream).use {
                it.writeBytes(objectMapper.writeValueAsString(PostMessage(channel, "Dude!")))
            }

            // Get Response
            val r = connection.inputStream.use { stream ->
                InputStreamReader(stream).useLines { it.joinToString(System.lineSeparator()) }
            }
            log.log("POST response: $r")
        } finally {
            connection?.disconnect()
        }
    }
}
