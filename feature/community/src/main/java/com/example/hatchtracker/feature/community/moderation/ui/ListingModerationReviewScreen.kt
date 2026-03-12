package com.example.hatchtracker.feature.community.moderation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.feature.community.R
import com.example.hatchtracker.core.ui.components.ScreenHeader
import com.example.hatchtracker.domain.model.ListingModerationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingModerationReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: ListingModerationViewModel = hiltViewModel()
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
                    Text("TRUST & SAFETY: MARKETPLACE REVIEW", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                ScreenHeader(title = "Listing Review", onBackClick = onNavigateBack)
            }
        }
    ) { padding ->
        val listing = uiState.listing
        if (uiState.isLoading || listing == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LISTING PREVIEW (Simplified)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = listing.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(R.string.moderation_listing_seller, listing.sellerUserId),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = listing.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                SectionHeader("Moderation Status")
                Surface(
                    color = when(listing.moderationState) {
                        ListingModerationState.ACTIVE -> Color.Green.copy(alpha = 0.1f)
                        ListingModerationState.MODERATION_HOLD -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = listing.moderationState.name,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                SectionHeader("Compliance Status")
                Text(listing.complianceStatus.name)

                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader("Actions")
                
                Button(
                    onClick = { viewModel.updateModerationState(ListingModerationState.ACTIVE, "Revoked hold / Released") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("APPROVE / RELEASE")
                }

                Button(
                    onClick = { viewModel.updateModerationState(ListingModerationState.MODERATION_HOLD, "Placed on moderation hold for review") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("PLACE ON HOLD")
                }

                OutlinedButton(
                    onClick = { viewModel.updateModerationState(ListingModerationState.BLOCKED, "Blocked for policy violation") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("BLOCK LISTING")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        fontWeight = FontWeight.Bold
    )
}
