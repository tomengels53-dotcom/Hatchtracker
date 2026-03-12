package com.example.hatchtracker.feature.profile

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.core.ui.composeutil.premiumClickable
import com.example.hatchtracker.core.common.asString
import coil.compose.AsyncImage
import com.example.hatchtracker.domain.model.UserProfile
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.feature.profile.ProfileUiState
import com.example.hatchtracker.feature.profile.UserProfileViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToProfileSetup: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val activity = LocalContext.current as? android.app.Activity
    var maintenanceDeviceId by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalUserProfileState provides uiState) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.user_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProfileUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.error_prefix, state.message), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { /* Reload logic if needed */ }) {
                            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.retry))
                        }
                    }
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        state = state,
                        onUpdateName = viewModel::updateDisplayName,
                        onChangePassword = viewModel::changePassword,
                        onDeleteAccount = viewModel::deleteAccount,
                        onUpdateLanguage = { tag ->
                            viewModel.updateLanguage(tag)
                            activity?.recreate()
                        },
                        onClearMessage = viewModel::clearMessage,
                        onNavigateToAddDevice = onNavigateToAddDevice,
                        onNavigateToSupport = { onNavigateToSupport() },
                        onNavigateToPaywall = onNavigateToPaywall,
                        onNavigateToProfileSetup = onNavigateToProfileSetup,
                        onMaintenanceClick = { deviceId ->
                            maintenanceDeviceId = deviceId
                        },
                        scrollState = scrollState
                    )
                }
                is ProfileUiState.AccountDeleted -> {
                    LaunchedEffect(Unit) {
                        onNavigateToProfileSetup() // Redirect to setup/login
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Text(stringResource(R.string.profile_msg_account_deleted), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }

        maintenanceDeviceId?.let { deviceId ->
            val state = uiState as? ProfileUiState.Success
            val device = state?.devices?.find { it.device.id == deviceId }?.device
            if (device != null) {
                val logs by viewModel.getMaintenanceLogs(deviceId).collectAsState(initial = emptyList())
                MaintenanceLogDialog(
                    deviceId = deviceId,
                    deviceName = device.displayName,
                    logs = logs,
                    onAddLog = viewModel::addMaintenanceLog,
                    onDeleteLog = { logId -> viewModel.deleteMaintenanceLog(deviceId, logId) },
                    onDismiss = { maintenanceDeviceId = null }
                )
            }
        }
    }
}
}

val LocalUserProfileState = staticCompositionLocalOf<ProfileUiState> { ProfileUiState.Loading }

@Composable
@Suppress("DEPRECATION")
fun ProfileContent(
    state: ProfileUiState.Success,
    onUpdateName: (String) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onDeleteAccount: () -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onClearMessage: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToProfileSetup: () -> Unit,
    onMaintenanceClick: (String) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val context = LocalContext.current
    var showNameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (state.message != null || state.error != null) {
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(state.message, state.error) {
            val msg = state.message ?: state.error
            if (msg != null) {
                snackbarHostState.showSnackbar(msg.asString(context))
                onClearMessage()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // A Profile Info Section
        ProfileInfoHeader(
            profile = state.profile,
            onEditName = { showNameDialog = true },
            onSetupProfile = onNavigateToProfileSetup
        )

        HorizontalDivider()

        // B Security Section
        SecuritySection(onPasswordClick = { showPasswordDialog = true })

        HorizontalDivider()

        // C Subscription Section
        SubscriptionSection(
            capabilities = state.capabilities,
            lastPlaySyncEpochMs = state.lastPlaySyncEpochMs,
            onManagePlanClick = onNavigateToPaywall
        )

        HorizontalDivider()

        // D Localization Section
        LocalizationSection(
            countryCode = state.profile.countryCode,
            currencyCode = state.profile.currencyCode,
            preferredLanguage = state.profile.preferredLanguage,
            onUpdateLanguage = onUpdateLanguage,
            onNavigateToSupport = { onNavigateToSupport() }
        )

        HorizontalDivider()

        // E Device Section
        DeviceSection(
            devices = state.devices,
            capabilities = state.capabilities,
            onAddDevice = onNavigateToAddDevice,
            onMaintenanceClick = onMaintenanceClick
        )
        
        HorizontalDivider()

        // F Danger Zone
        DangerZone(onDeleteClick = { showDeleteDialog = true })
    }

    if (showNameDialog) {
        EditNameDialog(
            currentName = state.profile.displayName,
            onDismiss = { showNameDialog = false },
            onConfirm = { 
                onUpdateName(it)
                showNameDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDeleteAccount()
                showDeleteDialog = false
            }
        )
    }
}

@Composable
fun ProfileInfoHeader(
    profile: UserProfile,
    onEditName: () -> Unit,
    onSetupProfile: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            if (profile.profilePictureUrl != null) {
                AsyncImage(
                    model = profile.profilePictureUrl,
                    contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.profile_picture),
                    modifier = Modifier.size(100.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            Surface(
                modifier = Modifier.size(32.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.change_picture),
                    modifier = Modifier.padding(8.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (profile.displayName.isNotBlank()) profile.displayName else androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.set_username),
                style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
            )
            IconButton(onClick = onEditName) {
                Icon(Icons.Default.Edit, contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.edit_name_content_desc), modifier = Modifier.size(24.dp))
            }
        }

        Text(
            text = profile.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onSetupProfile) {
            Text(stringResource(R.string.profile_button_complete_setup))
        }
    }
}

@Composable
fun SecuritySection(onPasswordClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.security_title), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .premiumClickable(onClick = onPasswordClick),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.change_password), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.update_credentials_desc), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SubscriptionSection(
    capabilities: com.example.hatchtracker.billing.SubscriptionCapabilities,
    lastPlaySyncEpochMs: Long,
    onManagePlanClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.subscription_title),
            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
        )
        Spacer(modifier = Modifier.height(8.dp))
        SubscriptionSummaryCard(
            tier = capabilities.tier,
            lastPlaySyncEpochMs = lastPlaySyncEpochMs,
            onManagePlanClick = onManagePlanClick
        )
    }
}

@Composable
fun SubscriptionSummaryCard(
    tier: SubscriptionTier,
    lastPlaySyncEpochMs: Long,
    onManagePlanClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.paywall_button_current_plan),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (tier) {
                        SubscriptionTier.FREE -> stringResource(R.string.paywall_tier_free)
                        SubscriptionTier.EXPERT -> stringResource(R.string.paywall_tier_expert)
                        SubscriptionTier.PRO -> stringResource(R.string.paywall_tier_pro)
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (lastPlaySyncEpochMs > 0L) {
                        stringResource(
                            R.string.paywall_play_sync_label,
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(Date(lastPlaySyncEpochMs))
                        )
                    } else {
                        stringResource(R.string.paywall_play_sync_never)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onManagePlanClick) {
                Text(stringResource(R.string.paywall_button_manage_subscription))
            }
        }
    }
}

@Composable
fun EditNameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.update_name_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.display_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.cancel)) }
        }
    )
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.current_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = new,
                    onValueChange = { new = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.confirm_new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(current, new) },
                enabled = new.length >= 6 && new == confirm
            ) { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.update)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.cancel)) }
        }
    )
}
@Composable
fun LocalizationSection(
    countryCode: String,
    currencyCode: String,
    preferredLanguage: String,
    onUpdateLanguage: (String) -> Unit,
    onNavigateToSupport: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguage = com.example.hatchtracker.common.localization.AppLanguage.fromTag(preferredLanguage)

    Column {
        Text(
            text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.localization_title),
            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Country & Currency (Immutable)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val countryName = com.example.hatchtracker.domain.breeding.LocalizationDefaults.getDefaultsForCountry(countryCode).name
                        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.current_country_format, countryName), style = MaterialTheme.typography.bodyMedium)
                        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.default_currency_format, currencyCode), style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = onNavigateToSupport) {
                        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.request_country_change))
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // Language (Mutable)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.app_language),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(currentLanguage.displayNameRes),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = { showLanguageDialog = true }) {
                        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.change_language))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.localization_warning),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.change_language)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    com.example.hatchtracker.common.localization.AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateLanguage(language.tag)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language.tag == preferredLanguage,
                                onClick = {
                                    onUpdateLanguage(language.tag)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(androidx.compose.ui.res.stringResource(language.displayNameRes))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.cancel))
                }
            }
        )
    }
}

@Deprecated("Legacy device tracking on UserProfile is being decentralized. Use Feature Hubs instead.")
@Suppress("DEPRECATION")
@Composable
fun DeviceSection(
    devices: List<com.example.hatchtracker.domain.breeding.DeviceCapacity>,
    capabilities: com.example.hatchtracker.billing.SubscriptionCapabilities,
    onAddDevice: () -> Unit,
    onMaintenanceClick: (String) -> Unit
) {
    val isPremium = capabilities.tier != com.example.hatchtracker.data.models.SubscriptionTier.FREE
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.my_devices_title), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onAddDevice) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.add))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        if (devices.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.no_devices_added), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            devices.forEach { capacity ->
                DeviceCard(
                    capacity = capacity,
                    isPremium = isPremium,
                    onMaintenanceClick = { onMaintenanceClick(capacity.device.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Deprecated("Legacy device tracking on UserProfile is being decentralized. Use Feature Hubs instead.")
@Composable
fun DeviceCard(
    capacity: com.example.hatchtracker.domain.breeding.DeviceCapacity,
    isPremium: Boolean,
    onMaintenanceClick: () -> Unit
) {
    val device = capacity.device
    val isDisposed = device.lifecycleStatus == com.example.hatchtracker.model.DeviceLifecycleStatus.DISPOSED
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisposed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon based on type
                Icon(
                    imageVector = if (device.type == com.example.hatchtracker.model.DeviceType.SETTER) Icons.Default.Star else Icons.Default.Info,
                    contentDescription = null, // decorative
                    modifier = Modifier.size(20.dp),
                    tint = if (isDisposed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.displayName,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                            color = if (isDisposed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        if (isDisposed) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                Text("DISPOSED", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (isPremium) {
                        // Analytics summary exposed from UI state
                        val summary = (LocalUserProfileState.current as? ProfileUiState.Success)?.deviceAnalytics?.get(device.id)
                        if (summary != null) {
                                val freqText = if (summary.maintenanceFrequency != null) 
                                    "%.1f/mo".format(summary.maintenanceFrequency) else stringResource(com.example.hatchtracker.feature.profile.R.string.value_not_available)
                            Text(
                                text = stringResource(
                                    com.example.hatchtracker.feature.profile.R.string.device_service_summary_format,
                                    freqText,
                                    summary.totalMaintenanceCost
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.device_capacity_format, capacity.usedCapacity, capacity.totalCapacity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Capacity Bar
            LinearProgressIndicator(
                progress = { if (capacity.totalCapacity > 0) capacity.usedCapacity.toFloat() / capacity.totalCapacity else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (isDisposed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else if (capacity.remainingCapacity == 0) MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (isPremium && !isDisposed) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    AssistChip(
                        onClick = onMaintenanceClick,
                        label = { Text("Service Log") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}






@Composable
fun DangerZone(onDeleteClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_danger_zone),
            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onDeleteClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.profile_delete_account_perma))
        }
    }
}

@Composable
fun DeleteAccountDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var deleteInput by remember { mutableStateOf("") }
    val isConfirmed = deleteInput.trim().uppercase() == "DELETE"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_dialog_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.profile_dialog_delete_copy),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.profile_dialog_delete_prompt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedTextField(
                    value = deleteInput,
                    onValueChange = { deleteInput = it },
                    placeholder = { Text(stringResource(R.string.profile_delete_keyword)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = deleteInput.isNotEmpty() && !isConfirmed
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isConfirmed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text(stringResource(R.string.profile_button_delete_forever)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(com.example.hatchtracker.feature.profile.R.string.cancel)) }
        }
    )
}
