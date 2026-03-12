"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handlePlayStoreNotification = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const billing_play_1 = require("./billing_play");
const firestore = admin.firestore();
/**
 * Handles Real-time Developer Notifications (RTDN) from Google Play.
 * Receives a Pub/Sub message or HTTPS POST, decodes it, and triggers verification.
 */
exports.handlePlayStoreNotification = functions.https.onRequest(async (req, res) => {
    try {
        let messageData;
        // 1. Determine payload logic (Pub/Sub push vs Direct)
        if (req.body.message) {
            // Pub/Sub Push format
            const buffer = Buffer.from(req.body.message.data, 'base64');
            messageData = JSON.parse(buffer.toString('utf-8'));
        }
        else {
            // Direct POST (unlikely for Play RTDN but good for testing)
            messageData = req.body;
        }
        console.log("RTDN Received:", JSON.stringify(messageData));
        const { packageName, subscriptionNotification, testNotification } = messageData;
        if (testNotification) {
            console.log("Received Test Notification from Google Play (v" + testNotification.version + ")");
            res.status(200).send("Test notification received.");
            return;
        }
        if (subscriptionNotification) {
            const { purchaseToken, subscriptionId } = subscriptionNotification;
            // 2. Lookup UID from purchaseToken
            // We expect the verifyPlaySubscription (Callable) to have seeded this mapping.
            const mappingDoc = await firestore.collection("playEntitlements").doc(purchaseToken).get();
            if (!mappingDoc.exists) {
                console.warn(`RTDN ignored: No user mapping found for token ${purchaseToken.substring(0, 10)}...`);
                // Return 200 anyway so Play doesn't retry forever.
                // Alternatively, we could search 'users' collection by subscriptionProductId + token if we stored it there,
                // but dedicated mapping is cleaner.
                res.status(200).send("No mapping found.");
                return;
            }
            const { uid, productId } = mappingDoc.data();
            console.log(`Verification triggered for user ${uid} (Product: ${productId}) via RTDN.`);
            // 3. Re-verify logic
            // Note: subscriptionId from notification should match productId from mapping.
            // But we use the stored one to be safe or the one from notification.
            // subscriptionNotification.subscriptionId IS the product ID.
            await (0, billing_play_1.verifySubscriptionInternal)(uid, purchaseToken, subscriptionId, packageName);
            console.log("RTDN validation complete.");
            res.status(200).send("Processed");
            return;
        }
        // Handle OneTimeProductNotification if needed
        console.log("Notification type not handled.");
        res.status(200).send("Processed (Ignored)");
    }
    catch (error) {
        console.error("RTDN Processing Error:", error);
        // If we fail, returning 500 makes Play retry. Be careful with loops.
        // Return 200 if it's a permanent error, 500 if transient.
        res.status(500).send("Internal Server Error");
    }
});
//# sourceMappingURL=play_rtdn.js.map