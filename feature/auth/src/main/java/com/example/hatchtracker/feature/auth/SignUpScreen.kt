package com.example.hatchtracker.feature.auth

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.core.common.asString
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.domain.policy.LegalConfig
import com.example.hatchtracker.domain.policy.SecurityPolicy
import com.example.hatchtracker.feature.auth.SignUpViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val termsAccepted by viewModel.termsAccepted.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSignUpSuccess by viewModel.isSignUpSuccess.collectAsState()
    val errorText = errorMessage?.asString(context)

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Navigation on Success
    LaunchedEffect(isSignUpSuccess) {
        if (isSignUpSuccess) {
            onSignUpSuccess()
        }
    }

    // Google Sign In Launcher (Duplicated logic from Login, bound to VM)
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
                    scope.launch {
                        val authResult = UserAuthManager.signInWithGoogle(credential)
                        if (authResult.isSuccess) {
                            viewModel.onGoogleSignInSuccess(authResult.getOrNull())
                        } else {
                            Toast.makeText(context, context.getString(R.string.auth_error_google_fail, authResult.exceptionOrNull()?.message ?: ""), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: ApiException) {
                // Silent catch or log
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
        // H E A D E R
        Text(
            text = stringResource(R.string.auth_title_signup),
            style = MaterialTheme.typography.displaySmall.copy(
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.auth_subtitle_signup),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // E M A I L
        OutlinedTextField(
            value = email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(stringResource(R.string.auth_label_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            isError = errorText?.contains("Email") == true
        )

        // P A S S W O R D
        OutlinedTextField(
            value = password,
            onValueChange = viewModel::onPasswordChange,
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            isError = errorText?.contains("Password") == true,
            supportingText = {
                 Text(stringResource(R.string.auth_hint_password_requirements), style = MaterialTheme.typography.labelSmall)
            }
        )

        // C O N F I R M   P A S S W O R D
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = { Text(stringResource(R.string.auth_label_confirm_password)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
             trailingIcon = {
                val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            isError = errorText?.contains("match") == true
        )

        // C O N S E N T
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = viewModel::onTermsAcceptedChange
            )
            
            // Styled Text for Terms
            val termsText = buildAnnotatedString {
                append(stringResource(R.string.auth_label_agree_to))
                
                val termsLink = androidx.compose.ui.text.LinkAnnotation.Url(
                    url = LegalConfig.TERMS_OF_SERVICE_URL,
                    styles = androidx.compose.ui.text.TextLinkStyles(
                        style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                    )
                )
                pushLink(termsLink)
                append(stringResource(R.string.terms_of_service))
                pop()
                
                append(stringResource(R.string.auth_label_and))
                
                val privacyLink = androidx.compose.ui.text.LinkAnnotation.Url(
                    url = LegalConfig.PRIVACY_POLICY_URL,
                    styles = androidx.compose.ui.text.TextLinkStyles(
                        style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                    )
                )
                pushLink(privacyLink)
                append(stringResource(R.string.privacy_policy))
                pop()
            }
            
            Text(
                text = termsText,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        }
        
        if (errorMessage != null) {
            Text(
                text = errorText.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
             Spacer(modifier = Modifier.height(16.dp))
        }

        // L A N G U A G E   S E L E C T O R
        val preferredLanguage by viewModel.preferredLanguage.collectAsState()
        var expanded by remember { mutableStateOf(false) }
        val currentLanguage = com.example.hatchtracker.common.localization.AppLanguage.fromTag(preferredLanguage)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            OutlinedTextField(
                value = stringResource(currentLanguage.displayNameRes),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(com.example.hatchtracker.core.ui.R.string.app_language_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                com.example.hatchtracker.common.localization.AppLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(stringResource(language.displayNameRes)) },
                        onClick = {
                            viewModel.onLanguageChange(language.tag)
                            expanded = false
                        }
                    )
                }
            }
        }

        // S I G N   U P   B U T T O N
        Button(
            onClick = { viewModel.signUp() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(R.string.auth_button_signup))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // G O O G L E
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
                    googleSignInClient.signOut()
                        .addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                } catch (e: Exception) {
                     // Error
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isLoading
        ) {
             Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_search),
                contentDescription = null, 
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.auth_button_signup_google))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // N A V   T O   L O G I N
        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(R.string.auth_button_goto_login))
        }
    }
}


