package com.livana.app.core.auth

import com.livana.app.core.model.UserProfile

/**
 * App-wide authentication state derived from Clerk's session plus the backend `/users/me` profile.
 *
 * - [Loading]  — Clerk is initializing or the profile is being resolved.
 * - [SignedOut] — no active Clerk session (or Clerk isn't configured).
 * - [SignedIn] — active session with the resolved backend profile (role + walletAddress cached).
 */
sealed interface AuthState {
    data object Loading : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val user: UserProfile) : AuthState
}
