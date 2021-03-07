package com.github.goodwillparking.robokash.slack

import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Auth {
    private val HEX_ARRAY: ByteArray = "0123456789ABCDEF".toByteArray(Charsets.US_ASCII)

    // https://api.slack.com/authentication/verifying-requests-from-slack#verifying-requests-from-slack-using-signing-secrets__a-recipe-for-security__how-to-make-a-request-signature-in-4-easy-steps-an-overview
    fun produceSignature(key: String, body: String, timestamp: Instant, version: String): String =
        "$version=${encode(key, "$version:${timestamp.epochSecond}:$body")}"

    private fun encode(key: String, data: String): String {
        val hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        hmac.init(secretKey)
        return hmac.doFinal(data.toByteArray(charset("UTF-8"))).toHexString()
    }

    // Copied from https://stackoverflow.com/a/9855338
    private fun ByteArray.toHexString(): String {
        val hexChars = ByteArray(size * 2)
        forEachIndexed { i, b ->
            val v: Int = b.toInt() and 0xFF
            hexChars[i * 2] = HEX_ARRAY[v ushr 4]
            hexChars[i * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars, Charsets.UTF_8)
    }
}
