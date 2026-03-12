package com.example.hatchtracker.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.feature.admin.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(
    onSelectBreeds: () -> Unit,
    onSelectTraits: () -> Unit,
    onSelectLogs: () -> Unit,
    onSelectTickets: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.admin.R.string.admin_tools_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.admin.R.string.desc_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onSelectTickets,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.admin.R.string.support_dashboard))
            }

            Button(
                onClick = onSelectBreeds,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.admin.R.string.manage_breeds))
            }
            
            Button(
                onClick = onSelectTraits,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.admin.R.string.trait_promotions))
            }
            
            Button(
                onClick = onSelectLogs,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.admin.R.string.audit_logs))
            }
        }
    }
}


