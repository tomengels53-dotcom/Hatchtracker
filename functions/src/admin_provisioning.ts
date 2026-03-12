
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * Provisions admin claims for users specified in the `admin.emails` config.
 * 
 * Usage:
 * 1. Set config: firebase functions:config:set admin.emails="user1@example.com,user2@example.com"
 * 2. Call this function via HTTPS Callable.
 */
export const provisionAdminClaims = functions.https.onCall(async (data: any, context: functions.https.CallableContext) => {
    // 1. Verify Caller is allowed to trigger this? 
    // Ideally this is a secured script, but for bootstrapping we might allow any auth user 
    // OR just checking if the caller is THE admin. 
    // For bootstrapping, we'll allow any authenticated user to trigger it, 
    // but the Logic only promotes specific emails in the config.

    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be authenticated.");
    }

    const adminConfig = functions.config().admin;
    if (!adminConfig || !adminConfig.emails) {
        throw new functions.https.HttpsError("failed-precondition", "Configuration 'admin.emails' not set.");
    }

    const adminEmails: string[] = (adminConfig.emails as string).split(",").map(e => e.trim());
    const results: string[] = [];

    for (const email of adminEmails) {
        try {
            const user = await admin.auth().getUserByEmail(email);

            // Set Custom Claims
            const currentClaims = user.customClaims || {};
            if (!currentClaims.isAdmin) {
                await admin.auth().setCustomUserClaims(user.uid, {
                    ...currentClaims,
                    isAdmin: true
                });
                results.push(`✅ Promoted ${email} (UID: ${user.uid})`);

                // Unify with Firestore
                await admin.firestore().collection("users").doc(user.uid).set({
                    roles: { admin: true },
                    isSystemAdmin: true,
                    lastUpdated: admin.firestore.FieldValue.serverTimestamp()
                }, { merge: true });

            } else {
                results.push(`ℹ️ ${email} is already an Admin.`);
            }

        } catch (error) {
            results.push(`❌ Failed to process ${email}: ${error instanceof Error ? error.message : 'Unknown error'}`);
        }
    }

    return {
        summary: results
    };
});
