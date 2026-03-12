package com.example.hatchtracker.feature.support

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgeDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.SupportTicket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTicketDetailPlaceholder(
    ticket: SupportTicket,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ticket_placeholder_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, colors = IconButtonDefaults.iconButtonColors()) {
                        Icon(Icons.Default.CheckCircle, contentDescription = stringResource(com.example.hatchtracker.core.ui.R.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        Surface(modifier = Modifier
            .padding(padding)
            .fillMaxWidth()) {
            Column(modifier = Modifier
                .padding(16.dp)) {
                Text(ticket.subject, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(ticket.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.ticket_status_label), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Badge(containerColor = BadgeDefaults.containerColor) {
                            Text(ticket.status.name)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ticket_close_view))
                }
            }
        }
    }
}
