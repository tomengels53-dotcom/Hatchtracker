// LAYOUT COMPOSITION CONTRACT
// This screen is restored to baseline commit 217a6a11.
// Do NOT alter:
// - Section ordering
// - Hero prominence
// - Grid structure
// - Spacing rhythm (24dp vertical / 16dp edges)
// - Card variant types
// without explicit design approval.
// LAYOUT COMPOSITION CONTRACT
// This screen conforms strictly to the pre-polish layout baseline.
// Do NOT alter container structure or component layout without explicit design approval.
package com.example.hatchtracker.feature.mainmenu.screens

import com.example.hatchtracker.core.ui.components.HatchyInsightPulse
import com.example.hatchtracker.core.ui.components.HatchyAssistButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hatchtracker.ads.AdManager
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.billing.SubscriptionCapabilities
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.core.navigation.NavRoute
import com.example.hatchtracker.core.ui.LockedFeatureInfo
import com.example.hatchtracker.core.ui.LockedFeatureProvider
import com.example.hatchtracker.core.ui.composeutil.AdViewFactory
import com.example.hatchtracker.core.ui.composeutil.BannerAd
import com.example.hatchtracker.core.ui.composeutil.LockedFeatureDialog
import com.example.hatchtracker.core.ui.composeutil.premiumClickable
// removed LifecycleHintCard import since it's defined locally now
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.feature.mainmenu.R
import com.example.hatchtracker.core.ui.R as UiR
import androidx.compose.ui.graphics.Color
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant
import com.example.hatchtracker.core.ui.components.AppSurfaceSpec
import com.example.hatchtracker.core.ui.components.FeatureHubCard
import com.example.hatchtracker.core.ui.components.FeatureHubScreenLayout
import com.example.hatchtracker.core.ui.components.HubAccent

import com.example.hatchtracker.feature.mainmenu.viewmodels.MainMenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    navController: NavController,
    onOpenDrawer: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToFlock: () -> Unit = {},
    onNavigateToIncubation: () -> Unit = {},
    onNavigateToBreeding: () -> Unit = {},
    onNavigateToNursery: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onNavigateToProduction: () -> Unit = {},
    viewModel: MainMenuViewModel = hiltViewModel()
) {

    var lockedFeatureInfoToShow by remember { mutableStateOf<LockedFeatureInfo?>(null) }

    if (lockedFeatureInfoToShow != null) {
        LockedFeatureDialog(
            info = lockedFeatureInfoToShow!!,
            onDismiss = { lockedFeatureInfoToShow = null },
            onLearnMore = {
                lockedFeatureInfoToShow = null
                onNavigateToPaywall()
            }
        )
    }



    val context = LocalContext.current
    val shouldShowAds by viewModel.shouldShowAds.collectAsState()
    val canAccessAdmin by viewModel.canAccessAdmin.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    // Initialize Ad SDK reactively
    LaunchedEffect(shouldShowAds) {
        if (shouldShowAds) {
            viewModel.initializeAds()
        }
    }

    // Remember the AdView only if ads should be shown
    val adView = remember(shouldShowAds) {
        if (shouldShowAds) {
            AdViewFactory.createBannerAdView(context, AdManager.BANNER_AD_UNIT_ID)
        } else {
            null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("MainMenuScreen"),
        topBar = {}
    ) { paddingValues ->
        // Security Alert for Developer
        val unauthorizedAdmins by viewModel.unauthorizedAdmins.collectAsState()
        if (unauthorizedAdmins.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(stringResource(UiR.string.security_alert_title)) },
                text = {
                    Column {
                        Text(stringResource(UiR.string.unauthorized_admins_msg), fontWeight = FontWeight.Bold)
                        Text(stringResource(UiR.string.verify_firestore_access_msg))
                    }
                },
                confirmButton = { TextButton(onClick = { }) { Text(stringResource(UiR.string.acknowledge_action)) } },
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        }

        Box(modifier = Modifier.padding(paddingValues)) {
                val currentCaps by viewModel.currentCapabilities.collectAsState()
                val homeContextState by viewModel.homeContextState.collectAsState()
                val assistNudgeKey by viewModel.assistNudgeKey.collectAsState()
                
                var showCommunityComingSoon by remember { mutableStateOf(false) }

                if (showCommunityComingSoon) {
                    AlertDialog(
                        onDismissRequest = { showCommunityComingSoon = false },
                        title = { Text(stringResource(UiR.string.community_coming_soon_title)) },
                        text = { Text(stringResource(UiR.string.community_coming_soon_body)) },
                        confirmButton = {
                            TextButton(onClick = { showCommunityComingSoon = false }) {
                                Text(stringResource(android.R.string.ok))
                            }
                        }
                    )
                }

                // Render Home Dashboard
                HomeMenuContent(
                    caps = currentCaps,
                    canAccessAdmin = canAccessAdmin,
                    isAdmin = isAdmin,
                    hasPremiumAccess = FeatureAccessPolicy
                        .canAccess(FeatureKey.SUPPORT, currentCaps.tier, canAccessAdmin)
                        .allowed,
                    homeContextState = homeContextState,
                    onNavigateToFlock = onNavigateToFlock,
                    onNavigateToIncubation = onNavigateToIncubation,
                    onNavigateToBreeding = onNavigateToBreeding,
                    onNavigateToNursery = onNavigateToNursery,
                    onNavigateToFinance = onNavigateToFinance,
                    onSettingsClick = onOpenDrawer,
                    onNavigateToPaywall = onNavigateToPaywall,
                    onNavigateToAdmin = onNavigateToAdmin,
                    onNavigateToProduction = onNavigateToProduction,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onNavigateToInsight = { insight ->
                        // Community features are dark-launched.
                        showCommunityComingSoon = true
                    },
                    onShowLockedFeature = { featureKey ->
                        lockedFeatureInfoToShow = LockedFeatureProvider.getLockedFeatureInfo(featureKey)
                    },
                    onCommunityComingSoonClick = { showCommunityComingSoon = true }
                )

                // Hatchy Chat Bubble (Floating Overlay)
                val dynamicBottomPadding = if (shouldShowAds) 84.dp else 20.dp
                com.example.hatchtracker.core.ui.components.HatchyAssistButton(
                    onClick = { navController.navigate(NavRoute.HatchyChat.route) },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .testTag("HatchyChatBubble"),
                    bottomPadding = dynamicBottomPadding,
                    nudgeKey = assistNudgeKey
                )
            }
        }
    }

@Composable
fun HomeMenuContent(
    caps: SubscriptionCapabilities,
    canAccessAdmin: Boolean,
    isAdmin: Boolean,
    hasPremiumAccess: Boolean,
    homeContextState: MainMenuViewModel.HomeContextState,
    onNavigateToFlock: () -> Unit,
    onNavigateToIncubation: () -> Unit,
    onNavigateToBreeding: () -> Unit,
    onNavigateToNursery: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToProduction: () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToInsight: (com.example.hatchtracker.domain.model.InsightFeedProjection) -> Unit,
    onShowLockedFeature: (FeatureKey) -> Unit,
    onCommunityComingSoonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top Toolbar-like Row for Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(UiR.string.settings_title),
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 1. Header Section (Centered Branding via HERO Card)
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            variant = AppCardVariant.HERO
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(60.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hatch),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Image(
                        painter = painterResource(id = UiR.drawable.hatchy_1),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.hen),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(UiR.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(UiR.string.app_tagline_hero),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        HomeContextCard(
            state = homeContextState,
            onStartBatch = onNavigateToIncubation,
            onNavigateToInsight = onNavigateToInsight
        )

        // Navigation Selector (Tab Buttons)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val dailySelected = selectedTab == 0
            val mgmtSelected = selectedTab == 1

            OutlinedButton(
                onClick = { onTabSelected(0) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (dailySelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (dailySelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(UiR.string.main_menu_tab_daily))
            }
            OutlinedButton(
                onClick = { onTabSelected(1) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (mgmtSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (mgmtSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(UiR.string.main_menu_tab_management))
            }
        }

        // 3. Iconic Module Grid (Split)
        if (selectedTab == 0) {
            DailyOperationsGrid(
                onNavigateToIncubation = onNavigateToIncubation,
                onNavigateToNursery = onNavigateToNursery,
                onNavigateToProduction = onNavigateToProduction
            )
        } else {
            ManagementGrid(
                onNavigateToFlock = onNavigateToFlock,
                onNavigateToBreeding = {
                    val canAccessBreeding = FeatureAccessPolicy
                        .canAccess(FeatureKey.BREEDING, caps.tier, canAccessAdmin)
                        .allowed
                    if (canAccessBreeding) {
                        onNavigateToBreeding()
                    } else {
                        onShowLockedFeature(FeatureKey.BREEDING)
                    }
                },
                onNavigateToFinance = {
                    val access = FeatureAccessPolicy
                        .canAccess(FeatureKey.FINANCE, caps.tier, canAccessAdmin)
                        .allowed
                    if (access) {
                        onNavigateToFinance()
                    } else {
                        onShowLockedFeature(FeatureKey.FINANCE)
                    }
                },
                onNavigateToAdmin = onNavigateToAdmin,
                isAdmin = isAdmin || canAccessAdmin
            )
        }

        // Community Module - Visible to all, Dark-Launched
        PremiumModuleTile(
            title = stringResource(UiR.string.module_community),
            description = stringResource(UiR.string.community_description),
            iconRes = R.drawable.community_garden,
            isLocked = false, // Not showing as locked while in "Coming Soon" phase
            onClick = onCommunityComingSoonClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(UiR.string.made_for_keepers_footer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DailyOperationsGrid(
    onNavigateToIncubation: () -> Unit,
    onNavigateToNursery: () -> Unit,
    onNavigateToProduction: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ModuleTile(
                title = stringResource(UiR.string.module_incubation),
                iconRes = R.drawable.hatch,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToIncubation
            )
            ModuleTile(
                title = stringResource(UiR.string.module_nursery),
                iconRes = UiR.drawable.chicken,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToNursery
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ModuleTile(
                title = stringResource(UiR.string.module_production),
                iconRes = UiR.drawable.whole_egg,
                modifier = Modifier.weight(0.5f),
                onClick = onNavigateToProduction
            )
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

@Composable
fun ManagementGrid(
    onNavigateToFlock: () -> Unit,
    onNavigateToBreeding: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    isAdmin: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ModuleTile(
                title = stringResource(UiR.string.module_flock),
                iconRes = R.drawable.hen,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToFlock
            )
            ModuleTile(
                title = stringResource(UiR.string.module_breeding),
                iconRes = R.drawable.cock,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToBreeding
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ModuleTile(
                title = stringResource(UiR.string.module_finance),
                iconRes = UiR.drawable.financial_profit,
                modifier = Modifier.weight(0.5f),
                onClick = onNavigateToFinance
            )
            if (isAdmin) {
                ModuleTile(
                    title = stringResource(UiR.string.module_admin_tools_short),
                    iconRes = UiR.drawable.admin,
                    modifier = Modifier.weight(0.5f),
                    titleStyle = MaterialTheme.typography.titleSmall,
                    onClick = onNavigateToAdmin
                )
            } else {
                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
fun ModuleTile(
    title: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    onClick: () -> Unit
) {
    AppCard(
        modifier = modifier
            .aspectRatio(1f)
            .premiumClickable(onClick = onClick),
        variant = AppCardVariant.STANDARD,
        shape = AppSurfaceSpec.ShapeSmallTile
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                androidx.compose.ui.graphics.Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset.Zero.copy(y = 0f), // Top Center (approx)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(bottom = 24.dp)
                )

                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PremiumModuleTile(
    title: String,
    description: String,
    iconRes: Int,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .premiumClickable(onClick = onClick),
        variant = AppCardVariant.STANDARD,
        colors = if (isLocked) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) else null
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(end = 20.dp),
                    alpha = if (isLocked) 0.5f else 1f
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isLocked)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Text(
                        text = if (isLocked) stringResource(UiR.string.available_on_pro_plans) else description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isLocked) 0.7f else 1f
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContextCard(
    state: MainMenuViewModel.HomeContextState,
    onStartBatch: () -> Unit,
    onNavigateToInsight: (com.example.hatchtracker.domain.model.InsightFeedProjection) -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        variant = AppCardVariant.STANDARD
    ) {
        when (state) {
            is MainMenuViewModel.HomeContextState.Loading -> {
                // Renders the same footprint as Lifecycle to avoid flicker
                LifecycleContent(onStartBatch = onStartBatch, isPlaceholder = true)
            }
            is MainMenuViewModel.HomeContextState.Lifecycle -> {
                LifecycleContent(onStartBatch = onStartBatch)
            }
            is MainMenuViewModel.HomeContextState.Insight -> {
                InsightContent(
                    insight = state.projection,
                    onClick = { onNavigateToInsight(state.projection) }
                )
            }
        }
    }
}

@Composable
private fun LifecycleContent(
    onStartBatch: () -> Unit,
    isPlaceholder: Boolean = false
) {
    Column(
        modifier = Modifier.padding(16.dp).then(if (isPlaceholder) Modifier.background(Color.Transparent) else Modifier),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(id = UiR.drawable.chick),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                alpha = if (isPlaceholder) 0.5f else 1f
            )
            Text(
                text = "Understanding the Lifecycle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color.Unspecified
            )
        }
        Text(
            text = "Eggs go to the Hatchery. Chicks move to the Nursery. Adults join the Flock. Track costs and sales at every stage of the journey.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) 
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onStartBatch,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPlaceholder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaceholder) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isPlaceholder) Color.Transparent else MaterialTheme.colorScheme.onPrimary
                ),
                shape = AppSurfaceSpec.ShapeButton
            ) {
                Text("Start my first batch")
            }
        }
    }
}

@Composable
private fun InsightContent(
    insight: com.example.hatchtracker.domain.model.InsightFeedProjection,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hatchy Identity Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HatchyInsightPulse(insightId = insight.id) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = UiR.drawable.hatchy_1),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hatchy Insight",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (insight.showBadge) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Text(
                    text = "HatchBase Assistant",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = insight.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = AppSurfaceSpec.ShapeButton
        ) {
            Text("View Details")
        }
    }
}


