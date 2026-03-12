package com.example.hatchtracker.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.core.ui.components.FinancialBarChart
import com.example.hatchtracker.core.ui.components.HatchyAssistButton
import com.example.hatchtracker.core.ui.components.PricingInsightCard

import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.data.util.FinancialChartUtil
import com.example.hatchtracker.domain.breeding.MarketType
import com.example.hatchtracker.domain.breeding.CurrencyUtils
import com.example.hatchtracker.domain.pricing.PricingSuggestion
import com.example.hatchtracker.domain.pricing.PricingSuggestionResult
import com.example.hatchtracker.domain.pricing.*
import com.example.hatchtracker.model.FinancialTrustLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialStatsScreen(
    onBackClick: () -> Unit,
    onNavigateToHatchy: () -> Unit,
    viewModel: FinancialStatsViewModel
) {
    val chartData by viewModel.chartData.collectAsState()
    val totalProfit by viewModel.totalProfit.collectAsState()
    val avgROI by viewModel.avgROI.collectAsState()
    val ownerType by viewModel.ownerType.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val ownerId by viewModel.ownerId.collectAsState()
    val timeBucket by viewModel.timeBucket.collectAsState()
    val availableItems by viewModel.availableItems.collectAsState()
    val caps by viewModel.currentCapabilities.collectAsState()
    val pricingSuggestionResult by viewModel.pricingSuggestionResult.collectAsState()
    val marginPercent by viewModel.marginPercent.collectAsState()
    val marketType by viewModel.marketType.collectAsState()
    val costPerBird by viewModel.costPerBird.collectAsState()
    val profitPerBatch by viewModel.profitPerBatch.collectAsState()
    val trustLevel by viewModel.trustLevel.collectAsState()
    val isAdmin by UserAuthManager.isSystemAdmin.collectAsState()
    val isDeveloper by UserAuthManager.isDeveloper.collectAsState()

    val canAccessFinance = FeatureAccessPolicy
        .canAccess(FeatureKey.FINANCE, caps.tier, isAdmin || isDeveloper)
        .allowed

    if (!canAccessFinance) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.finance_title_analytics)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_action))
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.finance_msg_analytics_unavailable))
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.finance_title_analytics)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_action))
                    }
                },
                actions = {
                    if (caps.canExportData) {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.finance_label_export))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = if (caps.tier == SubscriptionTier.PRO) stringResource(R.string.finance_label_pro_analytics) else stringResource(R.string.finance_label_expert_analytics),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(selected = ownerType == null, onClick = { viewModel.setOwnerType(null) }, label = { Text(stringResource(R.string.finance_filter_all)) })
                        FilterChip(selected = ownerType == "flock", onClick = { viewModel.setOwnerType("flock") }, label = { Text(stringResource(R.string.finance_filter_flocks)) })
                        FilterChip(selected = ownerType == "incubation", onClick = { viewModel.setOwnerType("incubation") }, label = { Text(stringResource(R.string.finance_filter_incubation)) })
                        FilterChip(selected = ownerType == "nursery", onClick = { viewModel.setOwnerType("nursery") }, label = { Text(stringResource(R.string.finance_filter_nursery)) })
                    }
                }

                if (ownerType != null && availableItems.isNotEmpty()) {
                    item {
                        var expanded by remember { mutableStateOf(false) }
                        val selectedItemName = availableItems.find { it.first == ownerId }?.second ?: "All Items"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedItemName)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.finance_label_all_items)) },
                                    onClick = {
                                        viewModel.setOwnerId(null)
                                        expanded = false
                                    }
                                )
                                availableItems.forEach { (id, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            viewModel.setOwnerId(id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnalyticsStatCard(
                            label = stringResource(R.string.finance_label_net_profit),
                            value = CurrencyUtils.formatCurrency(totalProfit, currencyCode),
                            valueColor = if (totalProfit >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsStatCard(
                            label = stringResource(R.string.finance_label_avg_roi),
                            value = "${String.format("%.1f", avgROI)}%",
                            valueColor = if (avgROI >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (costPerBird != null || profitPerBatch != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (costPerBird != null) {
                                AnalyticsStatCard(
                                    label = stringResource(R.string.finance_label_cost_per_bird),
                                    value = CurrencyUtils.formatCurrency(costPerBird!!, currencyCode),
                                    isEstimate = trustLevel == FinancialTrustLevel.ESTIMATED,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (profitPerBatch != null) {
                                val isHighTrust = trustLevel == FinancialTrustLevel.HIGH
                                AnalyticsStatCard(
                                    label = if (isHighTrust) stringResource(R.string.finance_label_profit_per_batch) else stringResource(R.string.finance_label_batch_profit_estimate),
                                    value = CurrencyUtils.formatCurrency(profitPerBatch!!, currencyCode),
                                    valueColor = if (profitPerBatch!! >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    isEstimate = !isHighTrust,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (caps.isPricingStrategyEnabled && ownerId != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.finance_label_market_strategy),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MarketType.entries.forEach { type ->
                                        FilterChip(
                                            selected = marketType == type,
                                            onClick = { viewModel.setMarketType(type) },
                                            label = { Text(stringResource(type.displayNameRes)) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = stringResource(R.string.finance_label_target_margin, marginPercent.toInt()),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Slider(
                                    value = marginPercent.toFloat(),
                                    onValueChange = { viewModel.setMarginPercent(it.toDouble()) },
                                    valueRange = 0f..100f,
                                    steps = 19
                                )
                            }
                        }
                    }

                    if (pricingSuggestionResult is PricingSuggestionResult.Available) {
                        val suggestion = (pricingSuggestionResult as PricingSuggestionResult.Available).suggestion
                        item {
                            PricingInsightCard(suggestion = suggestion, currencyCode = currencyCode)
                        }
                    } else if (pricingSuggestionResult is PricingSuggestionResult.Unavailable) {
                         item {
                            Text(
                                text = (pricingSuggestionResult as PricingSuggestionResult.Unavailable).message,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.finance_label_revenue_vs_costs),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FinancialBarChart(data = chartData)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                LegendItem(color = Color(0xFFE57373), label = stringResource(R.string.finance_legend_costs))
                                LegendItem(color = Color(0xFF81C784), label = stringResource(R.string.finance_legend_revenue))
                            }
                        }
                    }
                }

                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.finance_label_time_granularity), style = MaterialTheme.typography.labelMedium)
                        TabRow(
                            selectedTabIndex = when (timeBucket) {
                                FinancialChartUtil.TimeBucket.DAY -> 0
                                FinancialChartUtil.TimeBucket.WEEK -> 1
                                FinancialChartUtil.TimeBucket.MONTH -> 2
                            },
                            modifier = Modifier
                                .width(300.dp)
                                .padding(top = 8.dp),
                            containerColor = Color.Transparent
                        ) {
                            Tab(
                                selected = timeBucket == FinancialChartUtil.TimeBucket.DAY,
                                onClick = { viewModel.setTimeBucket(FinancialChartUtil.TimeBucket.DAY) },
                                text = { Text(stringResource(R.string.finance_tab_day)) }
                            )
                            Tab(
                                selected = timeBucket == FinancialChartUtil.TimeBucket.WEEK,
                                onClick = { viewModel.setTimeBucket(FinancialChartUtil.TimeBucket.WEEK) },
                                text = { Text(stringResource(R.string.finance_tab_week)) }
                            )
                            Tab(
                                selected = timeBucket == FinancialChartUtil.TimeBucket.MONTH,
                                onClick = { viewModel.setTimeBucket(FinancialChartUtil.TimeBucket.MONTH) },
                                text = { Text(stringResource(R.string.finance_tab_month)) }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.finance_msg_pricing_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }

            HatchyAssistButton(
                onClick = onNavigateToHatchy,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = paddingValues.calculateBottomPadding() + 16.dp)
            )
        }

    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun AnalyticsStatCard(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isEstimate: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isEstimate) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isEstimate) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isEstimate) valueColor.copy(alpha = 0.7f) else valueColor
            )
        }
    }
}
