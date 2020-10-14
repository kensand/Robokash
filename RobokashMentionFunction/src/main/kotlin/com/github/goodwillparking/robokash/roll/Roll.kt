package com.github.goodwillparking.robokash.roll

import com.github.goodwillparking.robokash.keyword.Keywords

sealed class Roll {

    data class GlobalRole(val probability: Double): Roll()
    data class KeywordRoll(val keywords: Keywords): Roll()
}
