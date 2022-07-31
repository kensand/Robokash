import { HttpApi, HttpMethod, PayloadFormatVersion } from '@aws-cdk/aws-apigatewayv2-alpha';
import { HttpLambdaIntegration } from '@aws-cdk/aws-apigatewayv2-integrations-alpha';
import { Duration, Stack } from 'aws-cdk-lib';
import { Code, Function, Runtime } from 'aws-cdk-lib/aws-lambda';
import { RetentionDays } from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

export interface RobokashStackProps {
    readonly slack: SlackIntegration
    readonly botUserId: string
    readonly probabilities?: Probabilities
}

export interface SlackIntegration {
    readonly accessToken: string
    readonly signingSecret: string
}

export interface Probabilities {
    readonly responseChance: number
    readonly maxReplyProbability: number
    readonly maxMentionReplyProbability: number
    readonly maxReplyProbabilityThreshold: number
}

export class RobokashStack extends Stack {
    constructor(scope: Construct, id: string, props: RobokashStackProps) {
        super(scope, id);

        const probabilities = props.probabilities ?? RobokashStack.DEFAULT_PROBABILITIES

        const slackEventHandler = new Function(this, "SlackEventHandler", {
            handler: "com.github.goodwillparking.robokash.slack.SlackEventHandler::handleRequest",
            code: Code.fromAsset("../build/libs/Robokash.jar"),
            runtime: Runtime.JAVA_11,
            memorySize: 512,
            timeout: Duration.seconds(30),
            retryAttempts: 0,
            reservedConcurrentExecutions: 2,
            logRetention: RetentionDays.SIX_MONTHS,
            environment: {
                "BOT_ACCESS_TOKEN": props.slack.accessToken,
                "BOT_SIGNING_SECRET": props.slack.signingSecret,
                "BOT_USER_ID": props.botUserId,
                "RESPONSE_CHANCE": probabilities.responseChance.toString(),
                "MAX_REPLY_PROBABILITY": probabilities.maxReplyProbability.toString(),
                "MAX_MENTION_REPLY_PROBABILITY": probabilities.maxMentionReplyProbability.toString(),
                "MAX_REPLY_PROBABILITY_THRESHOLD": probabilities.maxReplyProbabilityThreshold.toString()
            }
        })

        const lambdaIntegration = new HttpLambdaIntegration("LambdaIntegration", slackEventHandler, {
            // VERSION_1_0 maintains the original case of HTTP headers.
            payloadFormatVersion: PayloadFormatVersion.VERSION_1_0
        })

        const api = new HttpApi(this, "SlackEventApi", {
            description: `${id} API invoked for all slack events. https://api.slack.com/events-api`
        })

        api.addRoutes({
            integration: lambdaIntegration,
            methods: [HttpMethod.POST],
            path: "/robokash/slack/event"
        })
    }

    static readonly DEFAULT_PROBABILITIES: Probabilities = {
        responseChance: 0.02,
        maxReplyProbability: 0.01,
        maxMentionReplyProbability: 0.20,
        maxReplyProbabilityThreshold: 100
    }
}
