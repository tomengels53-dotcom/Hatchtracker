package com.example.hatchtracker.billing

import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.data.models.SubscriptionTier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAccessTest {

    @Test
    fun `breeding access is available for all tiers and admin`() {
        assertTrue(FeatureAccessPolicy.canAccess(FeatureKey.BREEDING, SubscriptionTier.FREE, isAdmin = false).allowed)
        assertTrue(FeatureAccessPolicy.canAccess(FeatureKey.BREEDING, SubscriptionTier.EXPERT, isAdmin = false).allowed)
        assertTrue(FeatureAccessPolicy.canAccess(FeatureKey.BREEDING, SubscriptionTier.PRO, isAdmin = false).allowed)
        assertTrue(FeatureAccessPolicy.canAccess(FeatureKey.BREEDING, SubscriptionTier.FREE, isAdmin = true).allowed)
    }

    @Test
    fun `finance access is expert or pro unless admin`() {
        assertFalse(FeatureAccessPolicy.canAccess(FeatureKey.FINANCE, SubscriptionTier.FREE, isAdmin = false).allowed)
        assertTrue(FeatureAccessPolicy.canAccess(FeatureKey.FINANCE, SubscriptionTier.EXPERT, isAdmin = false).allowed)
        assertTrue(FeatureAccessPolicy.canAccess(FeatureKey.FINANCE, SubscriptionTier.PRO, isAdmin = false).allowed)
        assertTrue(FeatureAccessPolicy.canAccess(FeatureKey.FINANCE, SubscriptionTier.FREE, isAdmin = true).allowed)
    }
}
