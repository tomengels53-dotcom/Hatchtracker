package com.example.hatchtracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.domain.policy.LegalConfig
import com.example.hatchtracker.domain.policy.SecurityPolicy
import com.example.hatchtracker.data.repository.UserRepository
import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.core.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _termsAccepted = MutableStateFlow(false)
    val termsAccepted: StateFlow<Boolean> = _termsAccepted.asStateFlow()

    private val _preferredLanguage = MutableStateFlow(com.example.hatchtracker.common.localization.AppLanguage.SYSTEM.tag)
    val preferredLanguage: StateFlow<String> = _preferredLanguage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<UiText?>(null)
    val errorMessage: StateFlow<UiText?> = _errorMessage.asStateFlow()

    private val _isSignUpSuccess = MutableStateFlow(false)
    val isSignUpSuccess: StateFlow<Boolean> = _isSignUpSuccess.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail.trim()
        _errorMessage.value = null
    }

    fun onPasswordChange(newPass: String) {
        _password.value = newPass
        _errorMessage.value = null
    }

    fun onConfirmPasswordChange(newPass: String) {
        _confirmPassword.value = newPass
        _errorMessage.value = null
    }

    fun onTermsAcceptedChange(accepted: Boolean) {
        _termsAccepted.value = accepted
        _errorMessage.value = null
    }

    fun onLanguageChange(tag: String) {
        _preferredLanguage.value = tag
    }

    fun signUp() {
        if (_isLoading.value) return

        val emailVal = _email.value
        val passVal = _password.value
        val confirmVal = _confirmPassword.value

        // 1. Validation
        if (emailVal.isBlank()) {
            _errorMessage.value = UiText.StringResource(R.string.auth_error_email_required)
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
             _errorMessage.value = UiText.StringResource(R.string.auth_error_email_invalid)
             return
        }
        if (passVal.isBlank()) {
            _errorMessage.value = UiText.StringResource(R.string.auth_error_password_required)
            return
        }
        if (!SecurityPolicy.isValidPassword(passVal)) {
            _errorMessage.value = UiText.StringResource(R.string.auth_error_password_weak, SecurityPolicy.getPasswordRequirementsDescription())
            return
        }
        if (passVal != confirmVal) {
            _errorMessage.value = UiText.StringResource(R.string.auth_error_passwords_mismatch)
            return
        }
        if (!_termsAccepted.value) {
            _errorMessage.value = UiText.StringResource(R.string.auth_error_consent_required)
            return
        }

        // 2. Execution
        _isLoading.value = true
        viewModelScope.launch {
            val result = UserAuthManager.signUpWithEmail(emailVal, passVal)
            
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    // 3. Profile Creation with Consent
                    val profileResult = userRepository.ensureProfileFromAuth(
                        user = user,
                        termsVersion = LegalConfig.TERMS_VERSION,
                        privacyVersion = LegalConfig.PRIVACY_VERSION,
                        preferredLanguage = _preferredLanguage.value
                    )
                    
                    if (profileResult.isSuccess) {
                        _isSignUpSuccess.value = true
                    } else {
                        val uiError = com.example.hatchtracker.core.common.FirestoreErrorMapper.userMessage(profileResult.exceptionOrNull())
                        _errorMessage.value = UiText.StringResource(R.string.auth_error_profile_setup_failed, uiError)
                    }
                } else {
                    _errorMessage.value = UiText.StringResource(R.string.auth_error_no_user_returned)
                }
            } else {
                _errorMessage.value = UiText.StringResource(R.string.auth_error_signup_failed)
            }
            _isLoading.value = false
        }
    }
    
    fun onGoogleSignInSuccess(user: com.google.firebase.auth.FirebaseUser?) {
        if (user == null) {
             _errorMessage.value = UiText.StringResource(R.string.auth_error_google_signin_failed)
             return
        }
        viewModelScope.launch {
             // Ensure profile exists with consent (auto-merges dev flags)
              val profileResult = userRepository.ensureProfileFromAuth(
                  user = user,
                  termsVersion = LegalConfig.TERMS_VERSION,
                  privacyVersion = LegalConfig.PRIVACY_VERSION,
                  preferredLanguage = _preferredLanguage.value
              )
              if (profileResult.isSuccess) {
                  _isSignUpSuccess.value = true
              } else {
                  _errorMessage.value = com.example.hatchtracker.core.common.FirestoreErrorMapper.userMessage(profileResult.exceptionOrNull())
              }
        }
    }
}








