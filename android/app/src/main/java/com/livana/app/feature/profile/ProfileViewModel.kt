package com.livana.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livana.app.core.auth.AuthState
import com.livana.app.core.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {

    val authState: StateFlow<AuthState> = sessionManager.authState

    fun signOut() {
        viewModelScope.launch { sessionManager.signOut() }
    }
}
