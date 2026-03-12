"use strict";
var _a;
Object.defineProperty(exports, "__esModule", { value: true });
exports.lemonSqueezyWebhook = exports.paddleWebhook = exports.stripeWebhook = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");
const stripe_1 = require("stripe");
const firestore = admin.firestore();
/**
 * Shared logic to process validated subscription events.
 * Updates Firestore idempotently using a transaction.
 */
async function processSubscriptionEvent(update) {
    const { uid, subscriptionTier, status, source } = update;
    if (!uid) {
        console.warn(`Webhook ignored: No uid provided.`);
        return;
    }
    const userRef = firestore.collection("users").doc(uid);
    await firestore.runTransaction(async (transaction) => {
        const userDoc = await transaction.get(userRef);
        // 1. Determine New State
        let newTier = subscriptionTier;
        let newIsActive = status === "active";
        let newAdsEnabled = status !== "active";
        if (status === "expired" || status === "canceled") {
            newTier = "free";
            newIsActive = false;
            newAdsEnabled = true;
        }
        // 2. Idempotency Check
        if (userDoc.exists) {
            const currentData = userDoc.data();
            if ((currentData === null || currentData === void 0 ? void 0 : currentData.subscriptionTier) === newTier &&
                (currentData === null || currentData === void 0 ? void 0 : currentData.subscriptionSource) === source &&
                (currentData === null || currentData === void 0 ? void 0 : currentData.subscriptionStatus) === status) {
                console.log(`[Idempotency] Skipping update for ${uid} - state already matches.`);
                return;
            }
        }
        // 3. Perform Update
        console.log(`Processing ${source} event for ${uid}: ${status} -> ${newTier}`);
        transaction.set(userRef, {
            subscriptionTier: newTier,
            subscriptionSource: source,
            subscriptionStatus: status,
            subscriptionActive: newIsActive,
            adsEnabled: newAdsEnabled,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
    });
}
/**
 * Maps Paddle Plan ID to internal Subscription Tier.
 */
function mapPaddlePlanToTier(planId) {
    if (planId.includes("expert") || planId === "12345")
        return "expert";
    if (planId.includes("pro") || planId === "67890")
        return "pro";
    return "free";
}
/**
 * Maps Lemon Squeezy Plan Name to internal Subscription Tier.
 */
function mapLemonPlanToTier(planName) {
    const formattedName = planName.toLowerCase();
    if (formattedName.includes("pro"))
        return "pro";
    if (formattedName.includes("expert"))
        return "expert";
    return "free";
}
// =================================================================================
// STRIPE WEBHOOK
// =================================================================================
const stripe = new stripe_1.default(process.env.STRIPE_SECRET_KEY || ((_a = functions.config().stripe) === null || _a === void 0 ? void 0 : _a.secret_key) || '', {
    apiVersion: '2023-10-16',
});
exports.stripeWebhook = functions.https.onRequest(async (req, res) => {
    var _a, _b, _c, _d;
    const sig = req.headers['stripe-signature'];
    const endpointSecret = process.env.STRIPE_WEBHOOK_SECRET || ((_a = functions.config().stripe) === null || _a === void 0 ? void 0 : _a.webhook_secret);
    let event;
    try {
        if (!sig || !endpointSecret) {
            throw new Error("Missing signature or secret.");
        }
        event = stripe.webhooks.constructEvent(req.rawBody, sig, endpointSecret);
    }
    catch (err) {
        console.error(`Stripe Signature Verification Failed: ${err.message}`);
        res.status(400).send(`Webhook Error: ${err.message}`);
        return;
    }
    try {
        if (event.type === 'customer.subscription.updated' ||
            event.type === 'customer.subscription.deleted' ||
            event.type === 'customer.subscription.created') {
            const subscription = event.data.object;
            // Map metadata.uid -> uid as requested 
            const uid = ((_b = subscription.metadata) === null || _b === void 0 ? void 0 : _b.uid) || ((_c = subscription.metadata) === null || _c === void 0 ? void 0 : _c.firebase_uid);
            if (!uid) {
                res.json({ received: true });
                return;
            }
            let status = "active";
            if (event.type === 'customer.subscription.deleted' || subscription.status === 'canceled' || subscription.status === 'unpaid') {
                status = "canceled";
            }
            const nickname = ((_d = subscription.items.data[0].plan.nickname) === null || _d === void 0 ? void 0 : _d.toLowerCase()) || "";
            let tier = "free";
            if (nickname.includes("pro"))
                tier = "pro";
            else if (nickname.includes("expert"))
                tier = "expert";
            await processSubscriptionEvent({
                uid,
                subscriptionTier: tier,
                status,
                source: "stripe"
            });
        }
        res.json({ received: true });
    }
    catch (err) {
        console.error("Stripe Webhook Error:", err);
        res.status(500).send("Internal Server Error");
    }
});
// =================================================================================
// PADDLE WEBHOOK
// =================================================================================
exports.paddleWebhook = functions.https.onRequest(async (req, res) => {
    const secret = process.env.PADDLE_WEBHOOK_SECRET;
    // 1. Verify Signature
    // Requirement: HMAC-SHA1 of JSON payload (rawBody) vs p_signature header
    if (!secret) {
        console.error("PADDLE_WEBHOOK_SECRET not set.");
        res.status(500).send("Configuration Error");
        return;
    }
    const signature = req.headers['p_signature'];
    if (!signature) {
        console.error("Missing p_signature header.");
        res.status(401).send("Missing Signature");
        return;
    }
    const hmac = crypto.createHmac('sha1', secret);
    const digest = hmac.update(req.rawBody).digest('hex');
    if (digest !== signature) {
        console.error("Paddle signature check failed.");
        res.status(401).send("Invalid Signature");
        return;
    }
    const body = req.body;
    try {
        const alert_name = body.alert_name;
        // 2. Extract UID
        // Requirement: Map payload.user_id -> uid
        const uid = body.user_id;
        if (uid) {
            // 3. Map Status
            // Requirement: "subscription_cancelled" -> "canceled", else "active"
            let status = "active";
            if (alert_name === 'subscription_cancelled') {
                status = "canceled";
            }
            // 4. Map Tier
            // Requirement: Map plan_id -> subscriptionTier using mapPaddlePlanToTier
            const tier = mapPaddlePlanToTier(body.plan_id || "");
            // 5. Process
            await processSubscriptionEvent({
                uid,
                subscriptionTier: tier,
                status,
                source: "paddle"
            });
        }
        else {
            console.warn("Paddle webhook missing user_id.");
        }
        res.json({ received: true });
    }
    catch (err) {
        console.error("Paddle Webhook Error:", err);
        res.status(500).send("Server Error");
    }
});
// =================================================================================
// LEMON SQUEEZY WEBHOOK
// =================================================================================
exports.lemonSqueezyWebhook = functions.https.onRequest(async (req, res) => {
    var _a;
    try {
        const secret = process.env.LEMON_SQUEEZY_WEBHOOK_SECRET || ((_a = functions.config().lemon_squeezy) === null || _a === void 0 ? void 0 : _a.webhook_secret);
        if (!secret) {
            console.error("LEMON_SQUEEZY_WEBHOOK_SECRET not set.");
            res.status(500).send("Configuration Error");
            return;
        }
        // 1. Verify Signature
        // Lemon Squeezy sends a X-Signature header containing the HMAC SHA256 signature of the raw body
        const signature = req.get('X-Signature') || '';
        const hmac = crypto.createHmac('sha256', secret);
        const digest = Buffer.from(hmac.update(req.rawBody).digest('hex'), 'utf8');
        const signatureBuffer = Buffer.from(signature, 'utf8');
        if (digest.length !== signatureBuffer.length || !crypto.timingSafeEqual(digest, signatureBuffer)) {
            console.error("Lemon Squeezy signature check failed.");
            res.status(401).send("Invalid Signature");
            return;
        }
        const payload = req.body;
        const { meta, data } = payload;
        if (meta.event_name.startsWith('subscription_')) {
            const attributes = data.attributes;
            const uid = attributes.customer_reference; // Passed via custom data/customer ID
            // Or typically passed in 'custom_data' -> { firebase_uid: ... } but user requested 'customer_reference' mapping.
            if (uid) {
                // Map Status
                let status = "active";
                if (attributes.status === 'cancelled' || attributes.status === 'expired') {
                    status = "canceled";
                }
                // Map Tier
                // Using helper logic: map plan_name to tier
                const planName = attributes.variant_name || attributes.plan_name || "";
                const tier = mapLemonPlanToTier(planName);
                await processSubscriptionEvent({
                    uid,
                    subscriptionTier: tier,
                    status,
                    source: "lemon_squeezy"
                });
            }
            else {
                console.warn("Lemon Squeezy webhook missing customer_reference (UID).");
            }
        }
        res.json({ received: true });
    }
    catch (err) {
        console.error("Lemon Squeezy Webhook Error:", err);
        res.status(500).send("Server Error");
    }
});
//# sourceMappingURL=webhooks.js.map