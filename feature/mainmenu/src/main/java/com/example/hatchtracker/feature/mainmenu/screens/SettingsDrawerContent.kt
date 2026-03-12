package com.example.hatchtracker.feature.mainmenu.screens

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.notifications.NotificationPreferences
import com.example.hatchtracker.core.ui.components.ThemeToggle
import com.example.hatchtracker.data.theme.ThemeRepository
import com.example.hatchtracker.domain.theme.ThemeMode
import com.example.hatchtracker.common.localization.LanguagePreferences
import com.example.hatchtracker.common.localization.LocaleManager
import com.example.hatchtracker.common.localization.AppLanguage
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.common.localization.SupportedLanguages
import com.example.hatchtracker.common.localization.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

enum class SettingsScreen { MAIN, NOTIFICATIONS }

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsDrawerContent(
    subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager,
    authClaimsRepository: com.example.hatchtracker.data.repository.AuthClaimsRepository,
    configRepository: com.example.hatchtracker.data.repository.ConfigRepository,
    themeRepository: ThemeRepository,
    languagePreferences: LanguagePreferences,
    localeManager: LocaleManager,
    onClose: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToDevTools: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(SettingsScreen.MAIN) }
    
    // Collect theme from repository
    val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.LIGHT)

    var notificationPrefs by remember { mutableStateOf<NotificationPreferences?>(null) }
    var masterNotifications by remember { mutableStateOf(true) }
    var incubationReminders by remember { mutableStateOf(true) }
    var nurseryReminders by remember { mutableStateOf(false) }
    var flockReminders by remember { mutableStateOf(false) }
    var pushNotifications by remember { mutableStateOf(true) }
    
    // Internal Dev Tools Gesture State
    var devToolTapCount by remember { mutableIntStateOf(0) }
    val isDevOrAdmin by subscriptionStateManager.isDeveloper.collectAsState()
    val isAdmin by subscriptionStateManager.isAdmin.collectAsState()
    val appAccessConfig by configRepository.observeAppAccessConfig().collectAsState(initial = null)
    val isCommunityEnabled = appAccessConfig?.communityConfig?.communityEnabled ?: false

    LaunchedEffect(context) {
        val prefs = withContext(Dispatchers.IO) {
            NotificationPreferences(context.applicationContext)
        }
        notificationPrefs = prefs
        masterNotifications = prefs.isNotificationsEnabled
        incubationReminders = prefs.isIncubationRemindersEnabled
        nurseryReminders = prefs.isNurseryRemindersEnabled
        flockReminders = prefs.isFlockRemindersEnabled
        pushNotifications = prefs.isPushNotificationsEnabled
    }


    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == SettingsScreen.NOTIFICATIONS) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "SettingsTransition"
    ) { screen ->
        when (screen) {
            SettingsScreen.MAIN -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_settings_action))
            }
        }

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Button
        OutlinedButton(
            onClick = onNavigateToProfile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.user_profile_action))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Section
        Text(
            text = stringResource(R.string.appearance_label),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.theme_mode_label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                ThemeToggle(
                    isDark = themeMode == ThemeMode.DARK,
                    onToggle = { isDark ->
                        scope.launch {
                            themeRepository.setTheme(
                                if (isDark) ThemeMode.DARK else ThemeMode.LIGHT
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
          // Localization state
    val appLanguageTag by languagePreferences.appLanguageTag.collectAsState(initial = "")
    val currentLanguage = remember(appLanguageTag) {
        SupportedLanguages.find { it.tag == appLanguageTag } ?: SupportedLanguages.first()
    }
    var showLanguageDialog by remember { mutableStateOf(false) }
                
                Text(
                    text = stringResource(R.string.app_language_label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(currentLanguage.labelRes), style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                if (showLanguageDialog) {
                    AlertDialog(
                        onDismissRequest = { showLanguageDialog = false },
                        title = { Text(stringResource(R.string.app_language_label)) },
                        text = {
                            Column {
                                SupportedLanguages.forEach { language ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    val tag = language.tag ?: ""
                                                    languagePreferences.setLanguageTag(tag)
                                                    localeManager.applyLanguage(tag)
                                                    showLanguageDialog = false
                                                    
                                                    // On some devices, setApplicationLocales might take a frame to propagate
                                                    // recreate() ensures the entire Compose tree is refreshed with new resources.
                                                    (context as? android.app.Activity)?.recreate()
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = language == currentLanguage,
                                            onClick = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(language.labelRes))
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLanguageDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.notifications_label),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { currentScreen = SettingsScreen.NOTIFICATIONS },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.notifications_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.notifications_manage_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }


        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Support Section
        Text(
            text = stringResource(R.string.support_label),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onNavigateToSupport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.help_support_action))
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isDevOrAdmin || isAdmin) {
            OutlinedButton(
                onClick = onNavigateToDevTools,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Community Dev Tools")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f)) // Push logout to bottom

        // Logout Section
        Button(
            onClick = { onSignOut() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.log_out_action))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer About
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.app_version_format, "1.0.0"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable {
                    if (isDevOrAdmin || isAdmin) {
                        devToolTapCount++
                        if (devToolTapCount >= 5) {
                            devToolTapCount = 0
                            onNavigateToDevTools()
                        }
                    }
                }
            )

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
SettingsScreen.NOTIFICATIONS -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentScreen = SettingsScreen.MAIN }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = stringResource(R.string.notifications_label),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Allow Notifications", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text("Master toggle for all alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = masterNotifications,
                                    onCheckedChange = {
                                        masterNotifications = it
                                        scope.launch(Dispatchers.IO) {
                                            notificationPrefs?.isNotificationsEnabled = it
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.cloud_push_notifications), style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(R.string.cloud_push_notifications_desc), style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = pushNotifications,
                                    onCheckedChange = {
                                        pushNotifications = it
                                        scope.launch(Dispatchers.IO) {
                                            notificationPrefs?.isPushNotificationsEnabled = it
                                        }
                                    },
                                    enabled = masterNotifications
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hatchy Reminders", style = MaterialTheme.typography.titleMedium)
                        
                        val allSelected = incubationReminders && nurseryReminders && flockReminders
                        TextButton(
                            onClick = {
                                val newState = !allSelected
                                incubationReminders = newState
                                nurseryReminders = newState
                                flockReminders = newState
                                scope.launch(Dispatchers.IO) {
                                    notificationPrefs?.isIncubationRemindersEnabled = newState
                                    notificationPrefs?.isNurseryRemindersEnabled = newState
                                    notificationPrefs?.isFlockRemindersEnabled = newState
                                }
                            },
                            enabled = masterNotifications
                        ) {
                            Text(if (allSelected) "Deselect All" else "Select All")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.incubation_milestones_label), style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(R.string.incubation_milestones_description), style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = incubationReminders,
                                    onCheckedChange = {
                                        incubationReminders = it
                                        scope.launch(Dispatchers.IO) {
                                            notificationPrefs?.isIncubationRemindersEnabled = it
                                        }
                                    },
                                    enabled = masterNotifications
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.nursery_guidance_label), style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(R.string.nursery_guidance_description), style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = nurseryReminders,
                                    onCheckedChange = {
                                        nurseryReminders = it
                                        scope.launch(Dispatchers.IO) {
                                            notificationPrefs?.isNurseryRemindersEnabled = it
                                        }
                                    },
                                    enabled = masterNotifications
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.flock_transitions_label), style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(R.string.flock_transitions_description), style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = flockReminders,
                                    onCheckedChange = {
                                        flockReminders = it
                                        scope.launch(Dispatchers.IO) {
                                            notificationPrefs?.isFlockRemindersEnabled = it
                                        }
                                    },
                                    enabled = masterNotifications
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Quiet Hours", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Quiet Hours", style = MaterialTheme.typography.bodyLarge)
                                Text("Not configured", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            // Placeholder disabled switch or nothing
                            Switch(checked = false, onCheckedChange = {}, enabled = false)
                        }
                    }
                }
            }
        }
    }
}
