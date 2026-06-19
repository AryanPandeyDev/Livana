package com.livana.app.core.auth

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.fetchToken
import com.livana.app.core.network.TokenProvider
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * [TokenProvider] backed by the current Clerk session. Returns the session JWT to attach as
 * `Authorization: Bearer <jwt>` (api ref §1), or `null` when signed out / Clerk isn't configured.
 *
 * The [TokenProvider] contract is synchronous but Clerk's token fetch ([Session.fetchToken]) is a
 * suspend call. We bridge with [runBlocking]: the [com.livana.app.core.network.AuthInterceptor]
 * runs on OkHttp's background dispatcher threads (never the main thread), so briefly blocking that
 * worker while Clerk returns a cached/refreshed token is safe. Clerk caches tokens internally, so
 * this does not hit the network on every request.
 */
class ClerkTokenProvider @Inject constructor() : TokenProvider {
    override fun currentToken(): String? = try {
        val session = Clerk.session ?: return null
        runBlocking {
            when (val result = session.fetchToken()) {
                is ClerkResult.Success -> result.value.jwt
                is ClerkResult.Failure -> null
            }
        }
    } catch (_: Throwable) {
        // Defensive: if Clerk isn't initialized (blank key) or token retrieval fails, send no token.
        null
    }
}
