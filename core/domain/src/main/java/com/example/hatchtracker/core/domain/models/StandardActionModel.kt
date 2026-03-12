package com.example.hatchtracker.core.domain.models

import com.example.hatchtracker.model.UiText

/**
 * Single Action Definition
 */
data class ActionItem(
    val label: UiText,
    val icon: Any,
    val onClick: () -> Unit,
    val requiresEntitlement: Boolean = false,
    val isDestructive: Boolean = false
)

/**
 * Standardized Action Hub Contract (Bottom Sheet Symmetry).
 * Ensures every feature's action sheet has the same layout hierarchy.
 */
data class StandardActionModel(
    val primaryActions: List<ActionItem> = emptyList(),
    val secondaryActions: List<ActionItem> = emptyList(),
    val financeActions: List<ActionItem> = emptyList(),
    val destructiveActions: List<ActionItem> = emptyList()
)
