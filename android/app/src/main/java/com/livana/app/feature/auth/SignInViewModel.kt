package com.livana.app.feature.auth

import androidx.lifecycle.ViewModel
import com.livana.app.core.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {

    /**
     * Called when Clerk's prebuilt auth flow completes. Clerk's `userFlow` will already be emitting
     * the new user, which [SessionManager] observes; we additionally trigger a refresh so the
     * backend `/users/me` profile (role + walletAddress) is resolved promptly before we navigate.
     */
    fun onAuthComplete() {
        sessionManager.refresh()
    }
}
