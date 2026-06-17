package com.livana.app.core.network

interface TokenProvider {
    fun currentToken(): String?
}

object NoOpTokenProvider : TokenProvider {
    override fun currentToken(): String? = null
}
