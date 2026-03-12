package com.example.hatchtracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.data.repository.UserRepository
import com.google.firebase.auth.AuthCredential
import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.core.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: UiText) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onGoogleSignInSuccess(
        credential: AuthCredential,
        termsVersion: String,
        privacyVersion: String
    ) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val authResult = UserAuthManager.signInWithGoogle(credential)
            if (authResult.isSuccess) {
                val user = authResult.getOrNull()
                if (user != null) {
                    val profileResult = userRepository.ensureProfileFromAuth(
                        user = user,
                        termsVersion = termsVersion,
                        privacyVersion = privacyVersion
                    )
                    if (profileResult.isSuccess) {
                        _uiState.value = LoginUiState.Success
                    } else {
                        _uiState.value = LoginUiState.Error(
                            com.example.hatchtracker.core.common.FirestoreErrorMapper.userMessage(profileResult.exceptionOrNull())
                        )
                    }
                } else {
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.auth_error_user_not_found))
                }
            } else {
                _uiState.value = LoginUiState.Error(
                    UiText.StringResource(R.string.auth_error_google_signin_failed)
                )
            }
        }
    }

    fun signInWithEmail(email: String, pass: String, termsVersion: String, privacyVersion: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = UserAuthManager.signInWithEmail(email, pass)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    val profileResult = userRepository.ensureProfileFromAuth(
                        user = user,
                        termsVersion = termsVersion,
                        privacyVersion = privacyVersion
                    )
                    if (profileResult.isSuccess) {
                        _uiState.value = LoginUiState.Success
                    } else {
                        _uiState.value = LoginUiState.Error(
                            com.example.hatchtracker.core.common.FirestoreErrorMapper.userMessage(profileResult.exceptionOrNull())
                        )
                    }
                } else {
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.auth_error_user_missing))
                }
            } else {
                _uiState.value = LoginUiState.Error(
                    UiText.StringResource(R.string.auth_error_signin_failed)
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
