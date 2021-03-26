package com.github.goodwillparking.robokash

import com.github.goodwillparking.robokash.slack.Responses
import kotlin.random.Random

interface ResponseCheck {
    val probability: Double

    fun getResponse(random: Random = Random): String
}

data class RandomResponseCheck(val responses: Responses, override val probability: Double) : ResponseCheck {
    override fun getResponse(random: Random) = responses.values.random(random)
}
