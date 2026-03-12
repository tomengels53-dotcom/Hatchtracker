package com.example.hatchtracker.feature.auth

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import android.os.StrictMode
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.data.repository.UserRepository
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.common.asString
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.GoogleAuthProvider
import com.example.hatchtracker.domain.policy.LegalConfig
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    isAuthenticated: Boolean,
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading = uiState is LoginUiState.Loading

    // Watch for Success state to navigate
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success || isAuthenticated) {
            onLoginSuccess()
            viewModel.resetState()
        } else if (uiState is LoginUiState.Error) {
            Toast.makeText(context, (uiState as LoginUiState.Error).message.asString(context), Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    viewModel.onGoogleSignInSuccess(
                        credential = credential,
                        termsVersion = LegalConfig.TERMS_VERSION,
                        privacyVersion = LegalConfig.PRIVACY_VERSION
                    )
                }
            } catch (e: ApiException) {
                 // Log or Toast
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo or Name
        Text(
            text = stringResource(R.string.auth_title_login),
            style = MaterialTheme.typography.displayMedium.copy(
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.auth_subtitle_login),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Form Fields
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.auth_label_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_label_password)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { 
                if (email.isNotBlank()) {
                     scope.launch {
                         val result = UserAuthManager.sendPasswordReset(email)
                         if (result.isSuccess) {
                             Toast.makeText(context, context.getString(R.string.auth_msg_reset_sent, email), Toast.LENGTH_SHORT).show()
                         } else {
                             Toast.makeText(context, context.getString(R.string.auth_error_reset_fail, result.exceptionOrNull()?.message ?: ""), Toast.LENGTH_SHORT).show()
                         }
                     }
                } else {
                    Toast.makeText(context, context.getString(R.string.auth_error_enter_email_reset), Toast.LENGTH_SHORT).show()
                }
            }) {
                Text(stringResource(R.string.auth_button_forgot_password), style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, context.getString(R.string.auth_error_fill_all_fields), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                viewModel.signInWithEmail(
                    email = email,
                    pass = password,
                    termsVersion = LegalConfig.TERMS_VERSION,
                    privacyVersion = LegalConfig.PRIVACY_VERSION
                )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(R.string.auth_button_login))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.auth_label_or),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        // Google Sign In Button
         OutlinedButton(
            onClick = {
                try {
                    val playServicesStatus = GoogleApiAvailability.getInstance()
                        .isGooglePlayServicesAvailable(context)
                    if (playServicesStatus != ConnectionResult.SUCCESS) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.auth_error_google_unconfigured),
                            Toast.LENGTH_LONG
                        ).show()
                        Logger.e(LogTags.AUTH, "Google Sign-In blocked: Play Services unavailable status=$playServicesStatus")
                        return@OutlinedButton
                    }
                    val clientId = context.getString(R.string.default_web_client_id)
                    if (clientId == "REPLACE_ME" || clientId.isBlank()) {
                        Logger.e(LogTags.AUTH, "Google Sign-In is not configured. Client ID is missing or a placeholder.")
                        Toast.makeText(context, context.getString(R.string.auth_error_google_unconfigured), Toast.LENGTH_LONG).show()
                        return@OutlinedButton
                    }
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(clientId)
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    // Force account picker instead of reusing the previously authorized Google account.
                    val originalPolicy = StrictMode.allowThreadDiskWrites()
                    try {
                        googleSignInClient.signOut()
                            .addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                    } finally {
                        StrictMode.setThreadPolicy(originalPolicy)
                    }
                } catch (e: Exception) {
                     Toast.makeText(context, context.getString(R.string.auth_error_reset_fail, e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isLoading
        ) {
             // Google Icon placeholder
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_search), 
                contentDescription = null, 
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.auth_button_login_google))
        }
        


        Spacer(modifier = Modifier.height(32.dp))

        // N A V   T O   S I G N U P
        TextButton(onClick = onNavigateToSignUp) {
            Text(stringResource(R.string.auth_button_goto_signup))
        }
    }
}





