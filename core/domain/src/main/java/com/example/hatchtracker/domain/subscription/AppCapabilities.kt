package com.example.hatchtracker.domain.subscription

import com.example.hatchtracker.data.models.SubscriptionTier

data class AppCapabilities(
    val tier: SubscriptionTier = SubscriptionTier.FREE
)
