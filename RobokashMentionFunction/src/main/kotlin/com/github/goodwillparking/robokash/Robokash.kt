package com.github.goodwillparking.robokash

import com.github.goodwillparking.robokash.keyword.Keywords
import com.github.goodwillparking.robokash.roll.Roll
import com.github.goodwillparking.robokash.roll.Roll.GlobalRole
import com.github.goodwillparking.robokash.roll.Roll.KeywordRoll
import kotlin.random.Random

data class Robokash(
    val responses: List<Response>,
    val message: Message,
    val rolls: Iterable<Roll>,
    val config: Config = Config()
) {

    fun decideResponse(random: Random = Random.Default): String? {
        return rolls.asSequence()
            .map {
                when (it) {
                    is GlobalRole -> decideResponse(random, it)
                    is KeywordRoll -> decideResponse(random, it)
                }
            }
            .filterNotNull()
            .firstOrNull()
    }

    fun decideResponse(random: Random, role: GlobalRole): String? =
        if (role(random, role.probability)) responses.random(random).contents else null

    fun decideResponse(random: Random, role: KeywordRoll): String? {
        return TODO()
    }

    fun role(random: Random, probability: Double): Boolean =
        when (probability) {
            0.0 -> false // Check for 0 in case the Random rolls a 1.0 exactly.
            else -> random.nextDouble(0.0, 1.0) + probability >= 1.0
        }

    data class Config(
        val rollMultiplier: Int = 1,
        val probabilityMultiplier: Double = 1.0
    )

    data class Message(val sender: User, val contents: String, val mentions: Iterable<User>)

    data class Response(val contents: String, val keywords: Keywords)
}
