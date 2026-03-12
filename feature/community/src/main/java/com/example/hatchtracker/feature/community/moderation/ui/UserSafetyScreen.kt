package com.example.hatchtracker.feature.community.moderation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.components.ScreenHeader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSafetyScreen(
    onNavigateBack: () -> Unit,
    viewModel: UserSafetyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val canAccess by viewModel.canAccess.collectAsState()

    if (!canAccess) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ACCESS DENIED", color = Color.Red)
        }
        return
    }

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.error).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("TRUST & SAFETY: USER INVESTIGATION", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                ScreenHeader(title = "User Safety State", onBackClick = onNavigateBack)
            }
        }
    ) { padding ->
        val state = uiState.safetyState
        if (uiState.isLoading || state == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // USER SUMMARY
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("User ID: ${state.userId}", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Strikes: ", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = state.strikeCount.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (state.strikeCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // RESTRICTIONS
                Text("Current Restrictions", style = MaterialTheme.typography.titleMedium)
                
                RestrictionItem("Full Suspension", state.suspensionUntil, state.isSuspended)
                RestrictionItem("Posting Restricted", state.postingRestrictedUntil)
                RestrictionItem("Commenting Restricted", state.commentingRestrictedUntil)
                RestrictionItem("Marketplace Restricted", state.marketplaceRestrictedUntil)

                if (state.moderatorNotes != null) {
                    Text("Moderator Notes", style = MaterialTheme.typography.titleMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.moderatorNotes ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestrictionItem(
    label: String,
    expiry: Long?,
    isForceActive: Boolean = false
) {
    val now = System.currentTimeMillis()
    val isActive = isForceActive || (expiry != null && expiry > now)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
            if (isActive) {
                Text(
                    text = if (expiry == null) "PERMANENT" else "Until ${formatDate(expiry)}",
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                Text("None", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
