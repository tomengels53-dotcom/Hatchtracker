import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { google } from "googleapis";


// Re-declare internal types if not shared yet
type SubscriptionTier = "free" | "expert" | "pro";

const firestore = admin.firestore();

interface VerifySubscriptionRequest {
    purchaseToken: string;
    productId: string;
    packageName: string;
}

interface VerifySubscriptionResponse {
    subscriptionTier: SubscriptionTier;
    adsEnabled: boolean;
    subscriptionExpiry: number;
}

const PRODUCT_TIER_MAPPING: Record<string, SubscriptionTier> = {
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
export async function verifySubscriptionInternal(uid: string, purchaseToken: string, productId: string, packageName: string) {
    try {
        const authClient = await google.auth.getClient({
            scopes: ["https://www.googleapis.com/auth/androidpublisher"],
        });

        const playDeveloperApi = google.androidpublisher({
            version: "v3",
            auth: authClient,
        } as any) as any;

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
            packageName, // Store packageName too
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

        let tier: SubscriptionTier = "free";
        let adsEnabled = true;

        // 4. SUBSCRIPTION MAPPING & ADS LOGIC
        if (!isExpired) {
            // Map product ID to internal tier
            const mappedTier = PRODUCT_TIER_MAPPING[productId];
            if (mappedTier) {
                tier = mappedTier;
                adsEnabled = false; // Ads disabled for paid tiers
            } else {
                // Fallback if product ID not recognized but valid purchase found (edge case)
                console.warn(`Unknown product ID verified: ${productId}`);
                tier = "expert"; // Default safe fallback
                adsEnabled = false;
            }
        } else {
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

    } catch (error: any) {
        console.error("Subscription verification failed:", error);

        // Differentiate errors
        if (error.message && error.message.includes("Purchase not found")) {
            throw new functions.https.HttpsError("not-found", "Purchase token not found.");
        }

        throw new functions.https.HttpsError(
            "internal",
            "Failed to verify subscription with Google Play."
        );
    }
}

export const verifyPlaySubscription = functions.https.onCall(
    async (data: VerifySubscriptionRequest, context: functions.https.CallableContext): Promise<VerifySubscriptionResponse> => {
        if (!context.auth) {
            throw new functions.https.HttpsError("unauthenticated", "The function must be called while authenticated.");
        }

        const uid = context.auth.uid;
        const { purchaseToken, productId, packageName } = data;

        if (!purchaseToken || !productId || !packageName) {
            throw new functions.https.HttpsError("invalid-argument", "Missing purchaseToken, productId, or packageName.");
        }

        return verifySubscriptionInternal(uid, purchaseToken, productId, packageName);
    }
);
