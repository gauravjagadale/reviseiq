package com.example.ui.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.ExperimentalTime

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object LoggedOut : AuthUiState
    data class LoggedIn(
        val userId: String,
        val email: String?,
        val displayName: String?,
        val createdAt: String?
    ) : AuthUiState
}

/** Emitted when the sign-in user changes on this device. */
data class AccountSwitch(val previousUserId: String?, val newUserId: String)

@OptIn(ExperimentalTime::class)
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authPrefs = application.getSharedPreferences("reviseiq_auth", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _accountSwitchRequest = MutableSharedFlow<AccountSwitch>(extraBufferCapacity = 1)
    val accountSwitchRequest: SharedFlow<AccountSwitch> = _accountSwitchRequest.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                SupabaseProvider.client.auth.sessionStatus.collect { status ->
                    _uiState.value = when (status) {
                        SessionStatus.Initializing -> AuthUiState.Loading
                        is SessionStatus.Authenticated -> {
                            val user = status.session.user
                            if (user == null) {
                                AuthUiState.LoggedOut
                            } else {
                                // Detect if a *different* account just signed in;
                                // the previous user's local data may need handling.
                                val previous = authPrefs.getString("key_last_signed_in_user", null)
                                if (previous != null && previous != user.id) {
                                    _accountSwitchRequest.tryEmit(AccountSwitch(previous, user.id))
                                }
                                authPrefs.edit().putString("key_last_signed_in_user", user.id).apply()
                                AuthUiState.LoggedIn(
                                    userId = user.id,
                                    email = user.email,
                                    displayName = (user.userMetadata?.get("full_name") as? JsonPrimitive)?.content
                                        ?: (user.userMetadata?.get("name") as? JsonPrimitive)?.content,
                                    createdAt = user.createdAt?.toString()
                                )
                            }
                        }
                        else -> AuthUiState.LoggedOut
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.LoggedOut
            }
        }
    }

    fun signIn(emailAddress: String, password: String) {
        viewModelScope.launch {
            setBusy(true)
            try {
                SupabaseProvider.client.auth.signInWith(Email) {
                    email = emailAddress
                    this.password = password
                }
            } catch (e: Exception) {
                _message.emit(e.message ?: "Sign-in failed. Please try again.")
            } finally {
                setBusy(false)
            }
        }
    }

    fun signUp(emailAddress: String, password: String) {
        viewModelScope.launch {
            setBusy(true)
            try {
                SupabaseProvider.client.auth.signUpWith(Email) {
                    email = emailAddress
                    this.password = password
                }
                if (SupabaseProvider.client.auth.sessionStatus.value !is SessionStatus.Authenticated) {
                    _message.emit("Confirmation email sent. Verify your inbox, then sign in.")
                }
            } catch (e: Exception) {
                _message.emit(e.message ?: "Could not create the account.")
            } finally {
                setBusy(false)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                SupabaseProvider.client.auth.signOut()
            } catch (e: Exception) {
                _message.emit("Sign out failed. Try again.")
            }
        }
    }

    fun resetPassword(emailAddress: String) {
        viewModelScope.launch {
            try {
                SupabaseProvider.client.auth.resetPasswordForEmail(emailAddress)
                _message.emit("Password reset email sent.")
            } catch (e: Exception) {
                _message.emit(e.message ?: "Could not send the reset email.")
            }
        }
    }

    fun signInWithGoogle(googleIdToken: String) {
        viewModelScope.launch {
            setBusy(true)
            try {
                SupabaseProvider.client.auth.signInWith(IDToken) {
                    idToken = googleIdToken
                    provider = Google
                }
            } catch (e: Exception) {
                _message.emit(e.message ?: "Google sign-in failed.")
            } finally {
                setBusy(false)
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        _isBusy.value = busy
    }
}
