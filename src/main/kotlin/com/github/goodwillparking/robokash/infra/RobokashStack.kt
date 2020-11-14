package com.github.goodwillparking.robokash.infra

import com.github.goodwillparking.robokash.App
import software.amazon.awscdk.core.Construct
import software.amazon.awscdk.core.Duration
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.FunctionProps
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.apigatewayv2.HttpApi
import software.amazon.awscdk.services.apigatewayv2.HttpApiProps
import software.amazon.awscdk.services.apigatewayv2.HttpMethod
import software.amazon.awscdk.services.apigatewayv2.LambdaProxyIntegration
import software.amazon.awscdk.services.apigatewayv2.LambdaProxyIntegrationProps
import software.amazon.awscdk.services.apigatewayv2.PayloadFormatVersion

class RobokashStack(scope: Construct, id: String) : Stack(scope, id) {

    init {
        val mentionFunction = Function(
            this,
            "MentionFunction",
            FunctionProps.builder()
                .handler("${App::class.java.name}::handleRequest")
                .code(Code.fromAsset("./build/libs/Robokash.jar"))
                .runtime(Runtime.JAVA_11)
                .memorySize(512)
                .timeout(Duration.seconds(30))
                .environment(
                    mapOf(
                        "BOT_ACCESS_TOKEN" to "{{resolve:ssm:robokash-dev-token:1}}",
                        "BOT_SIGNING_SECRET" to "{{resolve:ssm:robokash-dev-signing-secret:1}}",
                    )
                )
                .build()
        )

        val mentionIntegration = LambdaProxyIntegration(
            LambdaProxyIntegrationProps.builder()
                .handler(mentionFunction)
                // VERSION_1_0 maintains the original case of HTTP headers.
                .payloadFormatVersion(PayloadFormatVersion.VERSION_1_0)
                .build()
        )

        val mentionApi = HttpApi(this, "MentionApi")

        mentionApi.addRoutes(AddRoutesOptions.builder()
            .integration(mentionIntegration)
            .methods(listOf(HttpMethod.POST))
            .path("/robokash/mention")
            .build())
    }
}
