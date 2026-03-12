import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { triggerTransactionalEmail, generateEmailHtml } from "./email_utils";



// =================================================================================
// EXPORTED MODULES
// =================================================================================

// 1. Billing & Subscriptions
export { verifyPlaySubscription } from "./billing_play";
export { handlePlayStoreNotification } from "./play_rtdn";
export { stripeWebhook, paddleWebhook, lemonSqueezyWebhook } from "./webhooks";

// 2. Account Management
export { deleteAccount } from "./account_management";

// 3. Financials
export { onFinancialEntryWrite } from "./financial_aggregator";
export { provisionAdminClaims } from "./admin_provisioning";


// =================================================================================
// FIREBASE INITIALIZATION
// =================================================================================

if (admin.apps.length === 0) {
    admin.initializeApp();
}
const firestore = admin.firestore();


// =================================================================================
// HELPERS
// =================================================================================

/**
 * Determines the default currency for a country code if provider data is missing.
 */
function determineDefaultCurrency(countryCode: string): string {
    const euCountries = ["AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "EL", "ES", "FI", "FR", "HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK"];
    const code = countryCode.toUpperCase();
    if (code === "GB") return "GBP";
    if (euCountries.includes(code)) return "EUR";
    return "USD";
}


// =================================================================================
// USAGE COUNTERS (QUOTA ENFORCEMENT)
// =================================================================================

/**
 * Triggers when a user's purchase document is written (created/updated/deleted).
 * This can be used to sync subscription status to the user's main profile.
 */
export const onSubscriptionUpdated = functions.firestore.document("userPurchases/{uid}").onWrite(async (change: functions.Change<functions.firestore.DocumentSnapshot>, context: functions.EventContext) => {
    const uid = context.params.uid;

    if (!uid) {
        console.warn("onSubscriptionUpdated triggered without uid in context.params.");
        return;
    }

    const afterData = change.after.exists ? change.after.data() : null;

    // Example: Sync subscription status to user profile
    const userRef = firestore.collection('users').doc(uid);

    if (afterData && afterData.status === 'active') {
        await userRef.set({
            subscriptionTier: afterData.tier,
            subscriptionExpiry: afterData.expiryDate,
            subscriptionSource: afterData.source,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        console.log(`User ${uid} subscription updated to active.`);
    } else if (!afterData || afterData.status === 'expired' || afterData.status === 'cancelled') {
        // If document deleted or status is expired/cancelled, set to free
        await userRef.set({
            subscriptionTier: 'free',
            subscriptionExpiry: null,
            subscriptionSource: null,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        console.log(`User ${uid} subscription set to free.`);
    }
});

/**
 * Triggers when a new incubation is created.
 * Increments the user's usage counter.
 */
export const onIncubationCreated = functions.firestore
    .document('incubations/{incubationId}')
    .onCreate(async (snapshot: functions.firestore.QueryDocumentSnapshot, context: functions.EventContext) => {
        const data = snapshot.data();
        const ownerId = data?.ownerId;

        if (!ownerId) {
            console.warn("Incubation created without ownerId:", context.params.incubationId);
            return;
        }

        const userRef = firestore.collection('users').doc(ownerId);

        await userRef.set({
            usage: {
                incubationsCount: admin.firestore.FieldValue.increment(1)
            }
        }, { merge: true });
    });

/**
 * Triggers when an incubation is deleted.
 * Decrements the user's usage counter.
 */
export const onIncubationDeleted = functions.firestore
    .document('incubations/{incubationId}')
    .onDelete(async (snapshot: functions.firestore.QueryDocumentSnapshot, context: functions.EventContext) => {
        const data = snapshot.data();
        const ownerId = data?.ownerId;

        if (!ownerId) {
            return;
        }

        const userRef = firestore.collection('users').doc(ownerId);

        await userRef.set({
            usage: {
                incubationsCount: admin.firestore.FieldValue.increment(-1)
            }
        }, { merge: true });
    });


// =================================================================================
// ADMIN MANAGEMENT FUNCTIONS (Admin SDK & RBAC)
// =================================================================================

/**
 * Triggers when a new user is created via Firebase Authentication.
 * Initializes their Firestore user profile.
 */
export const onUserCreated = functions.auth.user().onCreate(async (user: admin.auth.UserRecord, context: functions.EventContext) => {
    console.log(`New user created: ${user.uid}, email: ${user.email}`);
    const userRef = firestore.collection("users").doc(user.uid);

    await userRef.set({
        email: user.email,
        displayName: user.displayName || user.email?.split('@')[0],
        photoURL: user.photoURL,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        lastLoginAt: admin.firestore.FieldValue.serverTimestamp(),
        subscriptionTier: "free", // Default to free tier
        adsEnabled: true,
        roles: {
            admin: false,
            moderator: false
        },
        usage: {
            incubationsCount: 0
        }
    }, { merge: true });
});

/**
 * Updates a user's roles and sets custom claims for auth tokens.
 * Only callable by users with existing 'isAdmin' claim.
 */
export const adminUpdateUserRole = functions.https.onCall(async (data: any, context: functions.https.CallableContext) => {
    // 1. Verify Caller Authorisation
    if (!context.auth || !context.auth.token.isAdmin) {
        throw new functions.https.HttpsError(
            "permission-denied",
            "Only global admins can modify user roles."
        );
    }

    const { targetUid, roles } = data; // roles: { admin?: boolean, moderator?: boolean }

    if (!targetUid || !roles) {
        throw new functions.https.HttpsError("invalid-argument", "Missing targetUid or roles.");
    }

    try {
        // 2. Update Firestore User Profile
        await firestore.collection("users").doc(targetUid).set({
            roles: roles,
            isSystemAdmin: roles.admin === true,
            isCommunityAdmin: roles.moderator === true,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        // 3. Set Custom Identity Claims (Auth Token)
        await admin.auth().setCustomUserClaims(targetUid, {
            isAdmin: roles.admin === true,
            isModerator: roles.moderator === true
        });

        console.log(`Admin ${context.auth.uid} updated roles for ${targetUid}:`, roles);
        return { success: true };
    } catch (error) {
        console.error("Role update failed:", error);
        throw new functions.https.HttpsError("internal", "Failed to update roles.");
    }
});

/**
 * Forces a subscription tier update for a user (e.g., granting tester access).
 */
export const adminOverrideSubscription = functions.https.onCall(async (data: any, context: functions.https.CallableContext) => {
    // 1. Verify Caller Authorisation
    if (!context.auth || !context.auth.token.isAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Unauthorized.");
    }

    const { targetUid, tier, expiryDays } = data;

    if (!targetUid || !tier) {
        throw new functions.https.HttpsError("invalid-argument", "Missing data.");
    }

    const expiryTime = Date.now() + (expiryDays || 30) * 24 * 60 * 60 * 1000;

    try {
        await firestore.collection("users").doc(targetUid).set({
            subscriptionTier: tier,
            subscriptionSource: "admin_override",
            subscriptionExpiry: expiryTime,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        console.log(`Admin ${context.auth.uid} forced tier ${tier} for ${targetUid}`);
        return { success: true, expiry: expiryTime };
    } catch (error) {
        throw new functions.https.HttpsError("internal", "Override failed.");
    }
});

/**
 * FEATURE: TRAIT PROMOTION
 * Aggregates observations and triggers promotion requests.
 */
export const onObservationCreated = functions.firestore
    .document("traitObservations/{obsId}")
    .onCreate(async (snapshot: functions.firestore.QueryDocumentSnapshot) => {
        const obsData = snapshot.data();
        const { traitId, breedId } = obsData;

        const observationsSnapshot = await firestore.collection("traitObservations")
            .where("traitId", "==", traitId)
            .where("breedId", "==", breedId)
            .get();

        const evidenceCount = observationsSnapshot.size;
        const PROMOTION_THRESHOLD = 25;

        if (evidenceCount >= PROMOTION_THRESHOLD) {
            const requestRef = firestore.collection("traitPromotionRequests").doc(`${breedId}_${traitId}`);
            const requestDoc = await requestRef.get();

            if (!requestDoc.exists || requestDoc.data()?.status !== "approved") {
                await requestRef.set({
                    traitId,
                    breedId,
                    evidenceCount,
                    confidenceScore: Math.min(evidenceCount / 50, 1.0), // Simple linear confidence
                    status: "pending",
                    lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
                    createdAt: requestDoc.exists ? requestDoc.data()?.createdAt : admin.firestore.FieldValue.serverTimestamp()
                }, { merge: true });
            }
        }
    });

export const adminApproveTraitPromotion = functions.https.onCall(async (data: any, context: functions.https.CallableContext) => {
    if (!context.auth || !context.auth.token.isAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Admin only.");
    }

    const { requestId } = data;
    const requestRef = firestore.collection("traitPromotionRequests").doc(requestId);
    const requestDoc = await requestRef.get();

    if (!requestDoc.exists) throw new functions.https.HttpsError("not-found", "Request not found.");

    const { traitId, breedId } = requestDoc.data()!;
    const breedRef = firestore.collection("breedStandards").doc(breedId);

    await firestore.runTransaction(async (transaction) => {
        const breedDoc = await transaction.get(breedRef);
        const existingFixedTraits = breedDoc.data()?.geneticProfile?.fixedTraits || [];

        if (!existingFixedTraits.includes(traitId)) {
            transaction.update(breedRef, {
                "geneticProfile.fixedTraits": [...existingFixedTraits, traitId],
                "lastUpdated": Date.now()
            });
        }

        transaction.update(requestRef, {
            status: "approved",
            lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
    });

    return { success: true };
});

export const adminRejectTraitPromotion = functions.https.onCall(async (data: any, context: functions.https.CallableContext) => {
    if (!context.auth || !context.auth.token.isAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Admin only.");
    }

    const { requestId, adminNotes } = data;
    await firestore.collection("traitPromotionRequests").doc(requestId).update({
        status: "rejected",
        adminNotes: adminNotes || "Insufficient evidence or community consensus.",
        lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true };
});

/**
 * FEATURE: GENETIC BASELINE
 * Ensures every BreedStandard has a minimum genetic profile.
 */
export const onBreedStandardWrite = functions.firestore
    .document("breedStandards/{breedId}")
    .onWrite(async (change: functions.Change<functions.firestore.DocumentSnapshot>, context: functions.EventContext) => {
        const data = change.after.exists ? change.after.data() : null;
        if (!data) return null; // Deleted

        // Check if baseline generation is needed
        const hasFixedTraits = (data.geneticProfile?.fixedTraits || []).length > 0;
        const hasInferredTraits = (data.geneticProfile?.inferredTraits || []).length > 0;

        if (!hasFixedTraits && !hasInferredTraits) {
            console.log(`Generating genetic baseline for breed: ${data.name}`);
            const inferred: string[] = [];

            // 1. Comb Type
            const comb = (data.combType || "").toLowerCase();
            if (comb === "pea") inferred.push("P_gene");
            else if (comb === "rose") inferred.push("R_gene");
            else if (comb === "walnut") inferred.push("P_gene", "R_gene");

            // 2. Feather Type
            const feather = (data.featherType || "").toLowerCase();
            if (feather === "frizzle") inferred.push("F_gene");
            else if (feather === "silkied") inferred.push("h_gene");

            // 3. Skin Color
            if ((data.skinColor || "").toLowerCase() === "black") inferred.push("Fm_gene");

            // 4. Egg Color
            const egg = (data.eggColor || "").toLowerCase();
            if (egg.includes("blue") || egg.includes("green")) inferred.push("O_gene");

            if (inferred.length > 0) {
                return change.after.ref.update({
                    "geneticProfile.inferredTraits": admin.firestore.FieldValue.arrayUnion(...inferred),
                    "geneticProfile.confidenceLevel": "LOW",
                    "lastUpdated": Date.now()
                });
            }
        }
        return null;
    });

/**
 * Consumes an approved "Change Country" ticket to securely update the user's profile.
 * - Verifies ticket ownership and approval status.
 * - Updates User Profile (country/currency).
 * - Updates Ticket (status=RESOLVED, consumed metadata).
 */
export const consumeCountryChangeTicket = functions.https.onCall(async (data: any, context: functions.https.CallableContext) => {
    // 1. AUTHENTICATION
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = context.auth.uid;
    const { ticketId, newCountry, newCurrency } = data;

    if (!ticketId || !newCountry) {
        throw new functions.https.HttpsError("invalid-argument", "Missing ticketId or newCountry.");
    }

    const ticketRef = firestore.collection("tickets").doc(ticketId);
    const userRef = firestore.collection("users").doc(uid);

    try {
        await firestore.runTransaction(async (transaction) => {
            const ticketDoc = await transaction.get(ticketRef);

            // 2. TICKET VALIDATION
            if (!ticketDoc.exists) {
                throw new functions.https.HttpsError("not-found", "Ticket not found.");
            }

            const ticketData = ticketDoc.data();
            if (!ticketData) throw new functions.https.HttpsError("not-found", "Ticket data missing.");

            // Verify Ownership
            if (ticketData.userId !== uid) {
                throw new functions.https.HttpsError("permission-denied", "Ticket does not belong to calling user.");
            }

            // Verify Status (Must be APPROVED)
            if (ticketData.status !== "APPROVED") {
                throw new functions.https.HttpsError("failed-precondition", `Ticket status is ${ticketData.status}, must be APPROVED.`);
            }

            // Verify Type (Must be COUNTRY_CHANGE)
            const changeRequest = ticketData.changeRequest;
            if (changeRequest?.type !== "COUNTRY_CHANGE") {
                throw new functions.https.HttpsError("invalid-argument", "Ticket is not a Country Change request.");
            }

            // Verify Content (match requested country)
            if (changeRequest.newValue !== newCountry) {
                throw new functions.https.HttpsError("invalid-argument", "Requested country does not match ticket approval.");
            }

            // 3. EXECUTE UPDATES

            // Update User Profile
            transaction.update(userRef, {
                countryCode: newCountry,
                currencyCode: newCurrency || determineDefaultCurrency(newCountry), // Use provided or derived
                supportTicketId: ticketId,
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            });

            // Update Ticket (Close/Consume it)
            transaction.update(ticketRef, {
                status: "RESOLVED",
                consumedAt: admin.firestore.FieldValue.serverTimestamp(),
                consumedBy: uid,
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
        });

        console.log(`User ${uid} successfully consumed ticket ${ticketId} to change country to ${newCountry}.`);
        return { success: true };

    } catch (error) {
        console.error("Failed to consume country change ticket:", error);
        if (error instanceof functions.https.HttpsError) {
            throw error;
        }
        throw new functions.https.HttpsError("internal", "Transaction failed.");
    }
});


// =================================================================================
// SUPPORT SYSTEM NOTIFICATIONS
// =================================================================================

/**
 * Triggers when a new support ticket is created.
 * Sends an email notification to the system admin.
 */
export const onTicketCreated = functions.firestore
    .document("tickets/{ticketId}")
    .onCreate(async (snapshot: functions.firestore.QueryDocumentSnapshot, context: functions.EventContext) => {
        const ticket = snapshot.data();
        const ticketId = context.params.ticketId;

        if (!ticket) return;

        console.log(`Processing new ticket notification for ${ticketId}`);

        // Get Support Email from config or default
        // In local dev, use a placeholder or check env config
        // const supportEmail = functions.config().support?.email || "developer@test.com";
        const supportEmail = process.env.SUPPORT_EMAIL || "tom@example.com"; // Hardcoded for this user context

        const subject = `[HatchTracker Support] New Ticket - ${ticket.category?.moduleName || "General"}`;

        const bodyContent = `
            <strong>Ticket ID:</strong> ${ticketId}<br/>
            <strong>User:</strong> ${ticket.userEmail} (${ticket.userId})<br/>
            <strong>Category:</strong> ${ticket.category?.moduleName} > ${ticket.category?.featureName}<br/>
            <strong>Priority:</strong> ${ticket.priority || "MEDIUM"}<br/>
            <hr/>
            <h3>Description:</h3>
            <p style="white-space: pre-wrap;">${ticket.description}</p>
            <hr/>
            <strong>Device:</strong> ${ticket.deviceInfo?.model || "Unknown"} (Android ${ticket.deviceInfo?.os_version || "?"})<br/>
            <strong>App Version:</strong> ${ticket.appVersion || "?"}
        `;

        try {
            await triggerTransactionalEmail(
                ticket.userId, // Associate audit log with the user who created it
                supportEmail,     // Send TO the admin
                "admin_ticket_alert",
                subject,
                generateEmailHtml("New Support Request", bodyContent),
                { ticketId: ticketId }
            );
        } catch (error) {
            console.error("Failed to send support notification:", error);
        }
    });

/**
 * AI/Hatchy Triage System.
 * Triggers on new ticket creation to analyze and classify.
 */
export const triageSupportTicket = functions.firestore
    .document("tickets/{ticketId}")
    .onCreate(async (snapshot: functions.firestore.QueryDocumentSnapshot, context: functions.EventContext) => {
        const ticket = snapshot.data();
        const ticketId = context.params.ticketId;

        if (!ticket || !ticket.description) return;

        // Avoid infinite loops if we write back to the same doc (though onCreate is once)
        // Check if already triaged to be safe
        if (ticket.hatchyTriage) return;

        try {
            // Lazy load to avoid circular dep issues in some envs
            const { triageTicket } = await import("./triage_utils");

            console.log(`Hatchy is analyzing ticket ${ticketId}...`);

            const result = await triageTicket(
                ticket.description,
                ticket.category || {}
            );

            // Write back to Firestore
            await snapshot.ref.set({
                hatchyTriage: {
                    ...result,
                    processedAt: admin.firestore.FieldValue.serverTimestamp(),
                    modelUsed: "hatchy-heuristic-v1"
                }
            }, { merge: true });

            console.log(`Hatchy triage complete for ${ticketId}: ${result.classification}`);

        } catch (error) {
            console.error("Hatchy triage failed:", error);
            await snapshot.ref.set({
                hatchyTriage: {
                    status: "failed",
                    error: error instanceof Error ? error.message : "Unknown error"
                }
            }, { merge: true });
        }
    });
