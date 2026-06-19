package com.livana.app.core.auth

import com.clerk.api.Clerk
import com.livana.app.BuildConfig
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.data.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Single source of truth for authentication state. Bridges Clerk's session (`Clerk.userFlow` +
 * `Clerk.isInitialized`) with the backend `/users/me` profile, exposing a single [AuthState] flow.
 *
 * When Clerk isn't configured (blank publishable key) the manager stays [AuthState.SignedOut] and
 * never touches the Clerk SDK, mirroring the init guard in `LivanaApplication`.
 */
@Singleton
class SessionManager @Inject constructor(
    private val userRepository: UserRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        if (BuildConfig.CLERK_PUBLISHABLE_KEY.isBlank()) {
            _authState.value = AuthState.SignedOut
        } else {
            observeClerk()
        }
    }

    /** Re-resolve the backend profile (e.g. after a sign-in completes). */
    fun refresh() {
        if (BuildConfig.CLERK_PUBLISHABLE_KEY.isBlank()) return
        scope.launch { resolveProfile() }
    }

    /** Sign out of Clerk and drop the cached profile. */
    suspend fun signOut() {
        runCatching { Clerk.auth.signOut() }
        _authState.value = AuthState.SignedOut
    }

    private fun observeClerk() {
        combine(Clerk.isInitialized, Clerk.userFlow) { initialized, user ->
            initialized to user
        }
            .onEach { (initialized, user) ->
                when {
                    !initialized -> _authState.value = AuthState.Loading
                    user == null -> _authState.value = AuthState.SignedOut
                    else -> resolveProfile()
                }
            }
            .launchIn(scope)
    }

    private suspend fun resolveProfile() {
        when (val result = userRepository.getMe()) {
            is LivanaResult.Success -> _authState.value = AuthState.SignedIn(result.value)
            is LivanaResult.Failure ->
                // A 401 means Clerk thinks we're signed in but the backend rejected the token —
                // treat the session as signed out. Other errors are transient; keep current state.
                if (result.error.isUnauthorized()) {
                    _authState.value = AuthState.SignedOut
                }
        }
    }
}

private fun DomainError.isUnauthorized(): Boolean = when (this) {
    is DomainError.Http -> httpStatus == HTTP_UNAUTHORIZED
    is DomainError.Backend -> httpStatus == HTTP_UNAUTHORIZED
    else -> false
}

private const val HTTP_UNAUTHORIZED = 401
