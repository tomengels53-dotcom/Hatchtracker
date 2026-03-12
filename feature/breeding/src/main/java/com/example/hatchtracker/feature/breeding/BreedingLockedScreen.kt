package com.example.hatchtracker.feature.breeding

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.example.hatchtracker.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingLockedScreen(
    onBack: () -> Unit,
    onViewPlans: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.breeding_module_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hatchy Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hatchy_1),
                    contentDescription = stringResource(R.string.hatchy_content_description),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        stringResource(R.string.breeding_locked_cluck),
                        color = MaterialTheme.colorScheme.primary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                    )
                    Text(
                        stringResource(R.string.breeding_locked_secret_sauce),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.breeding_locked_intro_copy),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Blurred Preview Section
            Text(
                stringResource(R.string.breeding_locked_preview_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Blurred Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .blur(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.breeding_locked_preview_pairing), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                        Text(stringResource(R.string.breeding_locked_preview_match))
                    }
                    HorizontalDivider()
                    Text(stringResource(R.string.breeding_locked_preview_outcome))
                    Text(stringResource(R.string.breeding_locked_preview_risk))
                    Text(stringResource(R.string.breeding_locked_preview_tip))
                }
                
                // Overlay Icon
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = stringResource(R.string.locked),
                        modifier = Modifier.padding(16.dp).size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Perks List
            Text(
                stringResource(R.string.breeding_locked_benefits_title),
                style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BreedingPerkItem(
                    title = stringResource(R.string.breeding_locked_benefit_1_title),
                    desc = stringResource(R.string.breeding_locked_benefit_1_desc)
                )
                BreedingPerkItem(
                    title = stringResource(R.string.breeding_locked_benefit_2_title),
                    desc = stringResource(R.string.breeding_locked_benefit_2_desc)
                )
                BreedingPerkItem(
                    title = stringResource(R.string.breeding_locked_benefit_3_title),
                    desc = stringResource(R.string.breeding_locked_benefit_3_desc)
                )
                BreedingPerkItem(
                    title = stringResource(R.string.breeding_locked_benefit_4_title),
                    desc = stringResource(R.string.breeding_locked_benefit_4_desc)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onViewPlans,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.breeding_button_upgrade_pro))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                stringResource(R.string.breeding_locked_footer),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun BreedingPerkItem(title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp).padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}



