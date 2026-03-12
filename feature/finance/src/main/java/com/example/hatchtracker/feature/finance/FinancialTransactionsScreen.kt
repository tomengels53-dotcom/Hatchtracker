package com.example.hatchtracker.feature.finance

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.common.util.CurrencyUtils
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.data.models.FinancialEntry
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialTransactionsScreen(
    onBackClick: () -> Unit,
    viewModel: FinancialTransactionsViewModel
) {
    val transactions by viewModel.transactions.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val dateFormat = "MMM dd, yyyy" // Simplified for now, could be passed from VM

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.financial_summary) + " - Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.finance_transactions_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions, key = { it.syncId }) { entry ->
                    TransactionItem(
                        entry = entry,
                        currencyCode = currencyCode,
                        dateFormat = dateFormat,
                        onDeleteClick = { viewModel.deleteEntry(entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    entry: FinancialEntry,
    currencyCode: String,
    dateFormat: String,
    onDeleteClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat(dateFormat, Locale.getDefault()) }
    val dateString = dateFormatter.format(Date(entry.date))
    val isCost = entry.type == "cost"
    val amountColor = if (isCost) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
    val amountPrefix = if (isCost) "-" else "+"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entry.category,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                    )
                    Badge(
                        containerColor = amountColor.copy(alpha = 0.1f),
                        contentColor = amountColor
                    ) {
                        Text(
                            text = if (isCost) stringResource(R.string.costs) else stringResource(R.string.revenue),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(
                        R.string.finance_transactions_amount,
                        amountPrefix,
                        CurrencyUtils.formatCurrency(entry.amount, currencyCode)
                    ),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                    color = amountColor
                )
                if (entry.quantity > 1) {
                    Text(
                        text = stringResource(R.string.finance_transactions_qty, entry.quantity),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_action),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
