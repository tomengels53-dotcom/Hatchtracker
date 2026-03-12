"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.verifyPlaySubscription = exports.verifySubscriptionInternal = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const googleapis_1 = require("googleapis");
const firestore = admin.firestore();
const PRODUCT_TIER_MAPPING = {
    "subscription_expert_monthly": "expert",
    "subscription_expert_yearly": "expert",
    "subscription_pro_monthly": "pro",
    "subscription_pro_yearly": "pro",
    "hatchtracker_basic": "expert",
    "hatchtracker_pro": "pro"
};
/**
 * Core verification logic, shared by Callable and RTDN.
 */
async function verifySubscriptionInternal(uid, purchaseToken, productId, packageName) {
    try {
        const authClient = await googleapis_1.google.auth.getClient({
            scopes: ["https://www.googleapis.com/auth/androidpublisher"],
        });
        const playDeveloperApi = googleapis_1.google.androidpublisher({
            version: "v3",
            auth: authClient,
        });
        const subscription = await playDeveloperApi.purchases.subscriptions.get({
            packageName,
            subscriptionId: productId,
            token: purchaseToken,
        });
        // --- 1. Save Token Mapping for RTDN ---
        // Independent of validity, we save the token->uid mapping so subsequent RTDNs can find the user.
        await firestore.collection("playEntitlements").doc(purchaseToken).set({
            uid,
            productId,
            packageName,
            lastVerifiedAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        // --------------------------------------
        // const currency = subscription.data.priceCurrencyCode || "USD"; // Unused
        // Pricing lookup (simplified)
        // Pricing lookup (simplified) - Keep comment for context but remove unused var
        // let unitPrice = 0;
        // const mappedTierMap = PRODUCT_TIER_MAPPING[productId] || "free";
        // if (mappedTierMap === "expert") unitPrice = 4.99;
        // if (mappedTierMap === "pro") unitPrice = 9.99;
        // expiryTimeMillis is a string in the API response
        const expiryTimeMillis = parseInt(subscription.data.expiryTimeMillis || "0");
        const currentTimeMillis = Date.now();
        const isExpired = expiryTimeMillis < currentTimeMillis;
        let tier = "free";
        let adsEnabled = true;
        // 4. SUBSCRIPTION MAPPING & ADS LOGIC
        if (!isExpired) {
            // Map product ID to internal tier
            const mappedTier = PRODUCT_TIER_MAPPING[productId];
            if (mappedTier) {
                tier = mappedTier;
                adsEnabled = false; // Ads disabled for paid tiers
            }
            else {
                // Fallback if product ID not recognized but valid purchase found (edge case)
                console.warn(`Unknown product ID verified: ${productId}`);
                tier = "expert"; // Default safe fallback
                adsEnabled = false;
            }
        }
        else {
            console.log(`Subscription expired for user ${uid}`);
            tier = "free";
            adsEnabled = true;
        }
        // 5. FIRESTORE UPDATE
        // Safe write: Admin SDK bypasses firestore.rules
        const userRef = firestore.collection("users").doc(uid);
        await userRef.set({
            subscriptionTier: tier,
            subscriptionSource: "google_play",
            subscriptionProductId: productId,
            subscriptionExpiry: expiryTimeMillis,
            adsEnabled: adsEnabled,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            // Helper for client-side syncing
            lastSubscriptionSync: Date.now()
        }, { merge: true });
        // Return full status
        return {
            subscriptionTier: tier,
            adsEnabled,
            subscriptionExpiry: expiryTimeMillis
        };
    }
    catch (error) {
        console.error("Subscription verification failed:", error);
        // Differentiate errors
        if (error.message && error.message.includes("Purchase not found")) {
            throw new functions.https.HttpsError("not-found", "Purchase token not found.");
        }
        throw new functions.https.HttpsError("internal", "Failed to verify subscription with Google Play.");
    }
}
exports.verifySubscriptionInternal = verifySubscriptionInternal;
exports.verifyPlaySubscription = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "The function must be called while authenticated.");
    }
    const uid = context.auth.uid;
    const { purchaseToken, productId, packageName } = data;
    if (!purchaseToken || !productId || !packageName) {
        throw new functions.https.HttpsError("invalid-argument", "Missing purchaseToken, productId, or packageName.");
    }
    return verifySubscriptionInternal(uid, purchaseToken, productId, packageName);
});
//# sourceMappingURL=billing_play.js.map