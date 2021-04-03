package com.github.goodwillparking.robokash.tools.extractor

import java.time.Instant


data class MessageWrapper(val message: String, val timestamp: Instant, val channel: String)

data class LoggedMessage(var text: String, val clientMsgId: String?, val user: String)

data class MessageFilter(val blockedMessages: List<String>, val regexps: List<Regex>)
