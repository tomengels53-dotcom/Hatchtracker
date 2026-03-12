
@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.feature.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.NotificationHelper
import com.example.hatchtracker.core.logging.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDateTime

@HiltViewModel
class NotificationDebugViewModel @Inject constructor(
    private val workManager: WorkManager
) : ViewModel() {

    private val _workInfos = MutableStateFlow<List<WorkInfo>>(emptyList())
    val workInfos: StateFlow<List<WorkInfo>> = _workInfos

    fun loadWorkInfos() {
        viewModelScope.launch {
            try {
                // Determine how to query all relevant work. 
                // We can query by tag or just get all.
                // "smart_incubation_checks", "hatch_daily_check", "hatch_env_monitor"
                // Plus dynamic tags "milestone_X", "hatch_notification_X"
                
                // Let's get by specific known tags first
                val tags = listOf(
                    "hatch_daily_check", 
                    "hatch_env_monitor"
                )
                
                val list = mutableListOf<WorkInfo>()
                tags.forEach { tag ->
                     list.addAll(workManager.getWorkInfosByTag(tag).await())
                }
                
                // Also try to get everything? Not easily possible without loop or unique ids.
                // Let's just list the global ones and maybe a sample of others if possible.
                // Actually, accessing internal DB is hard.
                // Let's just rely on what we know.
                
                _workInfos.value = list.sortedByDescending { it.nextScheduleTimeMillis }
            } catch (e: Exception) {
                Logger.e("NotificationDebug", "Failed to load work", e)
            }
        }
    }
    
    fun sendTestNotification(context: android.content.Context) {
        NotificationHelper.showGenericNotification(context, 999L, "Test Notification", "This is a test from Debug Screen.", true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDebugScreen(
    onBack: () -> Unit,
    viewModel: NotificationDebugViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val workInfos by viewModel.workInfos.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadWorkInfos()
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.sendTestNotification(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Debug") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadWorkInfos() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Button(
                onClick = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.sendTestNotification(context)
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        viewModel.sendTestNotification(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Trigger Test Notification")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Scheduled Work (Global)", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn {
                items(workInfos) { work ->
                    WorkInfoCard(work)
                }
            }
        }
    }
}

@Composable
fun WorkInfoCard(work: WorkInfo) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("ID: ${work.id}", style = MaterialTheme.typography.labelSmall)
            Text("State: ${work.state}", style = MaterialTheme.typography.bodyMedium)
            work.tags.forEach { tag ->
                Text("Tag: $tag", style = MaterialTheme.typography.labelSmall)
            }
            if (work.nextScheduleTimeMillis > 0) {
                 val next = java.time.Instant.ofEpochMilli(work.nextScheduleTimeMillis)
                     .atZone(java.time.ZoneId.systemDefault())
                     .toLocalDateTime()
                Text("Next Run: $next", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
