package com.github.goodwillparking.robokash.infra

import com.github.goodwillparking.robokash.slack.SlackEventHandler
import software.amazon.awscdk.core.Construct
import software.amazon.awscdk.core.Duration
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions
import software.amazon.awscdk.services.apigatewayv2.HttpApi
import software.amazon.awscdk.services.apigatewayv2.HttpApiProps
import software.amazon.awscdk.services.apigatewayv2.HttpMethod
import software.amazon.awscdk.services.apigatewayv2.PayloadFormatVersion
import software.amazon.awscdk.services.apigatewayv2.integrations.LambdaProxyIntegration
import software.amazon.awscdk.services.apigatewayv2.integrations.LambdaProxyIntegrationProps
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.FunctionProps
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.logs.RetentionDays

class RobokashStack(scope: Construct, id: String) : Stack(scope, id) {

    val slackEventHandler = Function(
        this,
        "SlackEventHandler",
        FunctionProps.builder()
            .handler("${SlackEventHandler::class.java.name}::handleRequest")
            .code(Code.fromAsset("./build/libs/Robokash.jar"))
            .runtime(Runtime.JAVA_11)
            .memorySize(512)
            .timeout(Duration.seconds(30))
            .retryAttempts(0)
            .reservedConcurrentExecutions(2)
            .logRetention(RetentionDays.SIX_MONTHS)
            .environment(
                mapOf(
                    "BOT_ACCESS_TOKEN" to "{{resolve:ssm:$id-token:1}}",
                    "BOT_SIGNING_SECRET" to "{{resolve:ssm:$id-signing-secret:1}}",
                    "BOT_USER_ID" to "{{resolve:ssm:$id-user-id:1}}",
                    "RESPONSE_CHANCE" to 0.01.toString(),
                    "MAX_REPLY_PROBABILITY" to 0.05.toString(),
                    "MAX_MENTION_REPLY_PROBABILITY" to 0.40.toString(),
                    "MAX_REPLY_PROBABILITY_THRESHOLD" to 100.toString(),
                )
            )
            .build()
    )

    val lambdaIntegration = LambdaProxyIntegration(
        LambdaProxyIntegrationProps.builder()
            .handler(slackEventHandler)
            // VERSION_1_0 maintains the original case of HTTP headers.
            .payloadFormatVersion(PayloadFormatVersion.VERSION_1_0)
            .build()
    )

    val slackEventApi = HttpApi(
        this,
        "SlackEventApi",
        HttpApiProps.builder()
            .description("$id API invoked for all slack events. https://api.slack.com/events-api")
            .build()
    ).apply {
        addRoutes(
            AddRoutesOptions.builder()
                .integration(lambdaIntegration)
                .methods(listOf(HttpMethod.POST))
                .path("/robokash/slack/event")
                .build()
        )
    }

}
