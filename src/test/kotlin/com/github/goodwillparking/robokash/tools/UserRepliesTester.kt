package com.github.goodwillparking.robokash.tools

import com.github.goodwillparking.robokash.UserSpecificResponseCache
import com.github.goodwillparking.robokash.slack.Responses
import com.github.goodwillparking.robokash.util.DefaultSerializer
import com.github.goodwillparking.robokash.util.ResourceUtil

fun main() {
    val responses = DefaultSerializer.objectMapper.readValue(
        ResourceUtil.loadTextResource("/responses.json"),
        Responses::class.java
    )

    val config = DefaultSerializer.objectMapper.readValue(
        ResourceUtil.loadTextResource("/user-specific-responses.json"),
        UserSpecificResponseCache.Config::class.java
    )

    val cache = config.users.map { it.slackId }.fold(UserSpecificResponseCache(responses, config)) { cache, user ->
        cache[user].first
    }

    println(cache.userResponses.mapValues { it.value?.size })
    println(
        cache.userResponses.entries.joinToString("\n\n") { (u, r) ->
            "${u.value}: ${r?.size} \n ${r?.values?.joinToString("\n")}"
        }
    )
}
