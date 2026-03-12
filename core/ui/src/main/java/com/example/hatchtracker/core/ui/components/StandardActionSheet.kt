package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.example.hatchtracker.core.ui.composeutil.premiumClickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.domain.models.ActionItem
import com.example.hatchtracker.core.domain.models.StandardActionModel
import com.example.hatchtracker.core.common.asString
import com.example.hatchtracker.model.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardActionSheet(
    title: UiText,
    actionModel: StandardActionModel,
    onDismiss: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.asString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = {
                    onDismiss()
                    onOpenDetails()
                }) {
                    Text(stringResource(com.example.hatchtracker.core.ui.R.string.details_action))
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            // Primary Actions (prominent buttons)
            if (actionModel.primaryActions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    actionModel.primaryActions.forEach { action ->
                        Button(
                            onClick = {
                                onDismiss()
                                action.onClick()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            val iconVector = action.icon as? ImageVector
                            if (iconVector != null) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(action.label.asString())
                        }
                    }
                }
            }

            // Secondary Actions (standard lists)
            if (actionModel.secondaryActions.isNotEmpty()) {
                actionModel.secondaryActions.forEach { action ->
                    ActionRow(action = action, onDismiss = onDismiss)
                }
            }

            // Financial Actions
            if (actionModel.financeActions.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ActionSectionHeader(stringResource(com.example.hatchtracker.core.ui.R.string.finance_tab))
                actionModel.financeActions.forEach { action ->
                    ActionRow(action = action, onDismiss = onDismiss)
                }
            }

            // Destructive Actions
            if (actionModel.destructiveActions.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ActionSectionHeader(stringResource(com.example.hatchtracker.core.ui.R.string.profile_danger_zone), color = MaterialTheme.colorScheme.error)
                actionModel.destructiveActions.forEach { action ->
                    ActionRow(action = action, onDismiss = onDismiss, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    action: ActionItem,
    onDismiss: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .premiumClickable(onClick = {
                onDismiss()
                action.onClick()
            })
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconVector = action.icon as? ImageVector
        if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = action.label.asString(),
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}

@Composable
private fun ActionSectionHeader(title: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}
