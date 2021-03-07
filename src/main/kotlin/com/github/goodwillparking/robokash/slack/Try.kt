package com.github.goodwillparking.robokash.slack

sealed class Try<T> {

    companion object {
        fun <T> of(block: () -> T) = try {
            Success(block())
        } catch (t: Throwable) {
            Failure(t)
        }
    }

    abstract fun getOrThrow(): T

    data class Success<T>(val value: T) : Try<T>() {
        override fun getOrThrow() = value
    }

    data class Failure<T>(val cause: Throwable) : Try<T>() {
        override fun getOrThrow() = throw cause
    }
}
