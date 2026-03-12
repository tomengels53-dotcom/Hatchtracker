package com.example.hatchtracker.feature.finance

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.billing.BillingManager
import com.example.hatchtracker.billing.BillingMapping
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.data.repository.BillingRepository
import com.example.hatchtracker.feature.finance.BillingViewModel
import com.example.hatchtracker.core.common.asString


import androidx.hilt.navigation.compose.hiltViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun PaywallScreen(
    reason: String? = null,
    onDismiss: () -> Unit = {},
    viewModel: BillingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val products by viewModel.products.collectAsState()
    val currentTier by viewModel.currentTier.collectAsState()
    val activePlayProductId by viewModel.activePlayProductId.collectAsState()
    val lastPlaySyncEpochMs by viewModel.lastPlaySyncEpochMs.collectAsState()
    
    // Track loading state
    var isInitialLoad by remember { mutableStateOf(true) }
    
    LaunchedEffect(products) {
        if (products.isNotEmpty()) {
            isInitialLoad = false
        }
    }

    // Handle Billing Events
    LaunchedEffect(Unit) {
        viewModel.billingEvents.collect { event ->
            when (event) {
                is BillingRepository.BillingEvent.Error -> {
                    android.widget.Toast.makeText(context, event.message.asString(context), android.widget.Toast.LENGTH_LONG).show()
                }
                is BillingRepository.BillingEvent.Message -> {
                    android.widget.Toast.makeText(context, event.message.asString(context), android.widget.Toast.LENGTH_SHORT).show()
                }
                is BillingRepository.BillingEvent.Success -> {
                    android.widget.Toast.makeText(context, context.getString(R.string.paywall_msg_purchase_success), android.widget.Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
                is BillingRepository.BillingEvent.Loading -> {}
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp), // Increase padding slightly
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 1. Header Changes
        Text(
            text = stringResource(R.string.paywall_title),
            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.paywall_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(40.dp)) // More breathing room
        
        // Loading / Error UI
        if (isInitialLoad && products.isEmpty()) {
            // Keep existing loading UI but update aesthetics slightly if needed? Keeping standard for now.
             Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.paywall_loading_options), style = MaterialTheme.typography.titleMedium)
                }
            }
        } else if (!isInitialLoad && products.isEmpty()) {
             Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.dialog_title_error), modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.paywall_error_load), style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { isInitialLoad = true; viewModel.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(top=16.dp)
                    ) { Text(stringResource(R.string.retry)) }
                }
            }
        }
        
        // 2. Plan Cards
        // Free
        PlanCard(
            title = stringResource(R.string.paywall_tier_free),
            price = stringResource(R.string.paywall_tier_free),
            buttonText = stringResource(R.string.paywall_button_current_plan),
            features = listOf(
                stringResource(R.string.paywall_feature_free_1), // Assuming I added it or need to add it
                stringResource(R.string.paywall_feature_free_2),
                stringResource(R.string.paywall_feature_free_3)
            ),
            isCurrent = currentTier == SubscriptionTier.FREE,
            isOwnedOrHigher = true, 
            onSelect = { }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val expertPlan = com.example.hatchtracker.billing.SubscriptionPlan.getForTier(SubscriptionTier.EXPERT)
        val proPlan = com.example.hatchtracker.billing.SubscriptionPlan.getForTier(SubscriptionTier.PRO)
        val currencyCode by viewModel.currencyCode.collectAsState()

        val isAtLeastExpert = currentTier == SubscriptionTier.EXPERT || currentTier == SubscriptionTier.PRO
        val isPro = currentTier == SubscriptionTier.PRO

        // Expert
        PlanCard(
            title = stringResource(R.string.paywall_tier_expert),
            price = "${com.example.hatchtracker.domain.breeding.CurrencyUtils.formatCurrency(expertPlan?.basePrice ?: 4.99, currencyCode)} / ${expertPlan?.period ?: "month"}",
            buttonText = stringResource(R.string.paywall_button_upgrade_expert),
            features = listOf(
                stringResource(R.string.paywall_feature_expert_1),
                stringResource(R.string.paywall_feature_expert_2),
                stringResource(R.string.paywall_feature_expert_3),
                stringResource(R.string.paywall_feature_expert_4),
                stringResource(R.string.paywall_feature_expert_5)
            ),
            isCurrent = currentTier == SubscriptionTier.EXPERT,
            isOwnedOrHigher = isAtLeastExpert,
            isRecommended = true,
            recommendationLabel = stringResource(R.string.paywall_label_most_used),
            onSelect = {
                if (activity != null) {
                    val product = products.find { it.productId == com.example.hatchtracker.billing.BillingMapping.PRODUCT_EXPERT_MONTHLY }
                    if (product != null) {
                        viewModel.buyProduct(activity, product, viewModel.getOfferToken(product))
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Pro
        PlanCard(
            title = stringResource(R.string.paywall_tier_pro),
            price = "${com.example.hatchtracker.domain.breeding.CurrencyUtils.formatCurrency(proPlan?.basePrice ?: 9.99, currencyCode)} / ${proPlan?.period ?: "year"}",
            buttonText = stringResource(R.string.paywall_button_upgrade_pro),
            features = listOf(
                stringResource(R.string.paywall_feature_pro_1),
                stringResource(R.string.paywall_feature_pro_2),
                stringResource(R.string.paywall_feature_pro_3),
                stringResource(R.string.paywall_feature_pro_4)
            ),
            isCurrent = currentTier == SubscriptionTier.PRO,
            isOwnedOrHigher = isPro,
            onSelect = {
                if (activity != null) {
                    val product = products.find { it.productId == com.example.hatchtracker.billing.BillingMapping.PRODUCT_PRO_YEARLY }
                    if (product != null) {
                        viewModel.buyProduct(activity, product, viewModel.getOfferToken(product))
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        if (lastPlaySyncEpochMs > 0L) {
            Text(
                text = stringResource(
                    R.string.paywall_play_sync_label,
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastPlaySyncEpochMs))
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 4. Actions (Secondary)
        TextButton(onClick = { 
            try {
                val targetUrl = if (!activePlayProductId.isNullOrBlank()) {
                    "https://play.google.com/store/account/subscriptions?sku=${activePlayProductId}&package=${context.packageName}"
                } else {
                    "https://play.google.com/store/account/subscriptions?package=${context.packageName}"
                }
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, context.getString(R.string.paywall_error_subscriptions_page), android.widget.Toast.LENGTH_SHORT).show()
            }
        }) {
            Text(stringResource(R.string.paywall_button_manage_subscription))
        }
        
        TextButton(onClick = { viewModel.restorePurchases() }) {
            Text(stringResource(R.string.paywall_button_restore_purchases), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Legal & Disclosure
        Text(
            text = stringResource(R.string.paywall_msg_legal_disclosure),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val legalText = androidx.compose.ui.text.buildAnnotatedString {
            // "By subscribing you agree to our %1$s and %2$s."
            val tosPart = stringResource(R.string.terms_of_service)
            val privacyPart = stringResource(R.string.privacy_policy)
            val fullText = stringResource(R.string.paywall_msg_agree_terms, tosPart, privacyPart)
            
            val tosIndex = fullText.indexOf(tosPart)
            val privacyIndex = fullText.indexOf(privacyPart)
            
            append(fullText)

            if (tosIndex != -1) {
                val termsLink = androidx.compose.ui.text.LinkAnnotation.Url(
                    url = com.example.hatchtracker.domain.policy.LegalConfig.TERMS_OF_SERVICE_URL,
                    styles = androidx.compose.ui.text.TextLinkStyles(
                        style = androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                    )
                )
                addLink(termsLink, tosIndex, tosIndex + tosPart.length)
            }

            if (privacyIndex != -1) {
                val privacyLink = androidx.compose.ui.text.LinkAnnotation.Url(
                    url = com.example.hatchtracker.domain.policy.LegalConfig.PRIVACY_POLICY_URL,
                    styles = androidx.compose.ui.text.TextLinkStyles(
                        style = androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                    )
                )
                addLink(privacyLink, privacyIndex, privacyIndex + privacyPart.length)
            }
        }

        Text(
            text = legalText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    buttonText: String,
    features: List<String>,
    isCurrent: Boolean,
    isOwnedOrHigher: Boolean = false,
    isRecommended: Boolean = false,
    recommendationLabel: String? = null,
    onSelect: () -> Unit
) {
    // 3. Highlighting: Neutral, not salesy. using surfaceContainer for all, distinction by border or tonal elevation?
    // Constraints: "Current plan: Slight border or tonal elevation, No bright colors", "Recommended: Small label, Neutral".
    
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    // Recommended gets a border? Or 'Current' gets border? 
    // "Current plan: Slight border..."
    val border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null
    // "Recommended plan: Small label..." - Maybe specific border for recommended too? 
    // Use tonal elevation for recommended? Unsure. Let's stick to standard card elevation + label.
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp), // More padding inside card
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRecommended && recommendationLabel != null) {
                Text(
                    text = recommendationLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // Neutral
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
            )
            
            Text(
                text = price,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary, // Natural primary color
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp).padding(top=4.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onSelect,
                enabled = !isOwnedOrHigher,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    when {
                        isCurrent -> stringResource(R.string.paywall_button_current_plan)
                        isOwnedOrHigher -> stringResource(R.string.paywall_button_owned)
                        else -> buttonText // "Upgrade to Expert/Pro"
                    }
                )
            }
        }
    }
}






