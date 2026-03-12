package com.example.hatchtracker.domain.entitlement

import com.example.hatchtracker.data.models.AppAccessConfig
import com.example.hatchtracker.data.models.SubscriptionTier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Model representing the result of an entitlement resolution.
 */
data class EntitlementResolution(
    val tier: SubscriptionTier,
    val reason: String
)

@Singleton
class EntitlementResolver @Inject constructor() {

    /**
     * Resolves the effective subscription tier based on billing status and reviewer configuration.
     * 
     * Deterministic Precedence:
     * 1. Billing (if active and not FREE)
     * 2. Custom Claims (if admin or dev)
     * 3. Reviewer Overlay (if active and UID is allowlisted)
     * 4. FREE fallback
     */
    fun resolve(
        billingActive: Boolean,
        realSubscriptionTier: SubscriptionTier,
        appAccessConfig: AppAccessConfig,
        currentUid: String?,
        isDeveloper: Boolean = false,
        isAdmin: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): EntitlementResolution {
        // 1. Billing check (Priority 1: Authority) - Always wins if active
        if (billingActive && realSubscriptionTier != SubscriptionTier.FREE) {
            return EntitlementResolution(
                tier = realSubscriptionTier,
                reason = "Active Google Play Subscription (${realSubscriptionTier.name})"
            )
        }

        // 2. Privilege elevation (Rank 2: Developer/Admin)
        // Access via custom claims
        if (isDeveloper || isAdmin) {
            val role = if (isAdmin) "Admin" else "Developer"
            return EntitlementResolution(
                tier = SubscriptionTier.PRO,
                reason = "Privileged Role Elevation ($role Claim)"
            )
        }

        // 3. Reviewer Overlay check (Priority 3)
        if (appAccessConfig.isOverlayActive(nowMillis)) {
            if (currentUid != null && currentUid in appAccessConfig.reviewerAllowlistUids) {
                return EntitlementResolution(
                    tier = appAccessConfig.reviewerTier,
                    reason = "Reviewer Overlay Active (UID Allowlisted, Tier: ${appAccessConfig.reviewerTier.name})"
                )
            } else {
                // We don't return here yet, just logical path
            }
        }

        // 4. Fallback
        val fallbackReason = if (appAccessConfig.isOverlayActive(nowMillis)) {
            "Overlay Active but UID not allowlisted; Fallback to FREE"
        } else if (billingActive && realSubscriptionTier == SubscriptionTier.FREE) {
             "Billing Active but Tier is FREE; Fallback to FREE"
        } else {
            "No active entitlements found; Fallback to FREE"
        }

        return EntitlementResolution(
            tier = SubscriptionTier.FREE,
            reason = fallbackReason
        )
    }
}
