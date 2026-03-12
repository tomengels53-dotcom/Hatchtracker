package com.example.hatchtracker.domain.entitlement

import com.example.hatchtracker.data.models.AppAccessConfig
import com.example.hatchtracker.data.models.SubscriptionTier
import org.junit.Assert.assertEquals
import org.junit.Test

class EntitlementResolverTest {

    private val resolver = EntitlementResolver()

    @Test
    fun `resolve returns real tier when billing is active`() {
        // Given
        val billingActive = true
        val realTier = SubscriptionTier.PRO
        val config = AppAccessConfig(
            reviewerOverlayEnabled = true,
            reviewerTier = SubscriptionTier.EXPERT,
            reviewerAllowlistUids = listOf("test-uid")
        )
        val uid = "test-uid"

        // When
        val result = resolver.resolve(billingActive, realTier, config, uid)

        // Then
        assertEquals(SubscriptionTier.PRO, result.tier)
    }

    @Test
    fun `resolve returns reviewer tier when billing inactive and overlay enabled and UID allowed`() {
        // Given
        val billingActive = false
        val realTier = SubscriptionTier.FREE
        val config = AppAccessConfig(
            reviewerOverlayEnabled = true,
            reviewerTier = SubscriptionTier.PRO,
            reviewerAllowlistUids = listOf("reviewer-uid")
        )
        val uid = "reviewer-uid"

        // When
        val result = resolver.resolve(billingActive, realTier, config, uid)

        // Then
        assertEquals(SubscriptionTier.PRO, result.tier)
    }

    @Test
    fun `resolve returns FREE when billing inactive and overlay enabled but UID NOT allowed`() {
        // Given
        val billingActive = false
        val realTier = SubscriptionTier.FREE
        val config = AppAccessConfig(
            reviewerOverlayEnabled = true,
            reviewerTier = SubscriptionTier.PRO,
            reviewerAllowlistUids = listOf("other-uid")
        )
        val uid = "unauthorized-uid"

        // When
        val result = resolver.resolve(billingActive, realTier, config, uid)

        // Then
        assertEquals(SubscriptionTier.FREE, result.tier)
    }

    @Test
    fun `resolve returns FREE when billing inactive and overlay disabled`() {
        // Given
        val billingActive = false
        val realTier = SubscriptionTier.FREE
        val config = AppAccessConfig(
            reviewerOverlayEnabled = false,
            reviewerTier = SubscriptionTier.PRO,
            reviewerAllowlistUids = listOf("reviewer-uid")
        )
        val uid = "reviewer-uid"

        // When
        val result = resolver.resolve(billingActive, realTier, config, uid)

        // Then
        assertEquals(SubscriptionTier.FREE, result.tier)
    }

    @Test
    fun `resolve returns FREE when billing inactive and UID is null`() {
        // Given
        val billingActive = false
        val realTier = SubscriptionTier.FREE
        val config = AppAccessConfig(
            reviewerOverlayEnabled = true,
            reviewerTier = SubscriptionTier.PRO,
            reviewerAllowlistUids = listOf("some-uid")
        )
        val uid: String? = null

        // When
        val result = resolver.resolve(billingActive, realTier, config, uid)

        // Then
        assertEquals(SubscriptionTier.FREE, result.tier)
    }

    @Test
    fun `resolve returns reviewer tier when overlay is enabled and not expired`() {
        // Given
        val billingActive = false
        val realTier = SubscriptionTier.FREE
        val nowMillis = 1_000L
        val config = AppAccessConfig(
            reviewerOverlayEnabled = true,
            reviewerTier = SubscriptionTier.PRO,
            reviewerAllowlistUids = listOf("reviewer-uid"),
            expiresAt = nowMillis + 1
        )
        val uid = "reviewer-uid"

        // When
        val result = resolver.resolve(
            billingActive, realTier, config, uid,
            nowMillis = nowMillis
        )

        // Then
        assertEquals(SubscriptionTier.PRO, result.tier)
    }

    @Test
    fun `resolve returns FREE when overlay is expired`() {
        // Given
        val billingActive = false
        val realTier = SubscriptionTier.FREE
        val nowMillis = 1_000L
        val config = AppAccessConfig(
            reviewerOverlayEnabled = true,
            reviewerTier = SubscriptionTier.PRO,
            reviewerAllowlistUids = listOf("reviewer-uid"),
            expiresAt = nowMillis - 1
        )
        val uid = "reviewer-uid"

        // When
        val result = resolver.resolve(
            billingActive, realTier, config, uid,
            nowMillis = nowMillis
        )

        // Then
        assertEquals(SubscriptionTier.FREE, result.tier)
    }
}

