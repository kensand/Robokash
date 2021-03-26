package com.github.goodwillparking.robokash

import com.github.goodwillparking.robokash.slack.Responses
import com.github.goodwillparking.robokash.slack.UserId
import com.github.goodwillparking.robokash.util.DefaultSerializer.objectMapper
import com.github.goodwillparking.robokash.util.ResourceUtil.loadTextResource

data class UserSpecificResponseCache(
    val responses: Responses,
    val config: Config,
    val userResponses: Map<UserId, Responses?> = emptyMap()
) {
    operator fun get(user: UserId): Pair<UserSpecificResponseCache, Responses?> =
        userResponses[user]?.let { this to it } ?: run {
            val foundResponses = findResponses(user)
            val found = if (foundResponses.isNotEmpty()) Responses(foundResponses) else null
            copy(userResponses = userResponses + (user to found)) to found
        }

    private fun findResponses(user: UserId): List<String> {
        val regexps = config.mappedConfig[user]?.messageRegexps ?: emptyList()
        return regexps.flatMap { regex -> responses.values.filter { regex in it } }.distinct()
    }

    data class Config(val users: List<UserConfig>) {

        val mappedConfig by lazy { users.associateBy(UserConfig::slackId) }
    }

    data class UserConfig(val slackId: UserId, val messageRegexps: List<Regex>)
}
