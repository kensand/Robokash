package com.github.goodwillparking.robokash

import com.github.goodwillparking.robokash.slack.UserId

class User(val slackId: UserId, val aliases: Set<String>)
