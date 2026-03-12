import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const firestore = admin.firestore();

/**
 * Permanently deletes a user's account and all associated data.
 * This function is irreversible.
 * 
 * Logic:
 * 1. Verify Authentication
 * 2. Require explicit confirmation ("DELETE")
 * 3. Delete Firestore Data (Recursive or Batched)
 * 4. Delete Auth User
 * 5. Log Audit Event
 */
export const deleteAccount = functions.https.onCall(async (data: any, context: functions.https.CallableContext) => {
    // 1. AUTHENTICATION
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = context.auth.uid;
    const { confirm } = data;

    // 2. CONFIRMATION
    if (confirm !== "DELETE") {
        throw new functions.https.HttpsError("invalid-argument", "Confirmation 'DELETE' is required.");
    }

    console.log(`Initiating account deletion for user: ${uid}`);

    try {
        // 3. FIRESTORE DELETION
        // Helper to delete collection by query field
        // If the collection can have subcollections, we must use recursiveDelete on EACH document.
        const deleteCollectionRecursive = async (collectionName: string, fieldName: string, fieldValue: string = uid) => {
            const collectionRef = firestore.collection(collectionName);
            let query = collectionRef.where(fieldName, "==", fieldValue).limit(100);

            let snapshot = await query.get();
            while (snapshot.size > 0) {
                const promises = snapshot.docs.map(doc => firestore.recursiveDelete(doc.ref));
                await Promise.all(promises);
                snapshot = await query.get();
            }
        };

        // 1. Delete User Document (and all its subcollections: devices, financials, etc.)
        const userRef = firestore.collection("users").doc(uid);
        await firestore.recursiveDelete(userRef);

        // 2. Delete Single-Doc User Collections (where docId == uid)
        // e.g. userPurchases/{uid} if it exists
        await firestore.collection("userPurchases").doc(uid).delete().catch(() => { });

        // 3. Delete Top-Level Collections (direct ownership)
        await deleteCollectionRecursive("tickets", "userId");
        await deleteCollectionRecursive("incubations", "userId");
        await deleteCollectionRecursive("traitObservations", "userId");
        await deleteCollectionRecursive("breedingScenarios", "ownerUserId");
        await deleteCollectionRecursive("playEntitlements", "uid"); // Delete mapping
        await deleteCollectionRecursive("auditLogs", "targetUid"); // Delete previous logs concernig this user
        await deleteCollectionRecursive("auditLogs", "performedBy"); // Delete logs of actions taken by this user

        // 4. Cascade Delete (Flocks -> Birds)
        // Find flocks first, then delete their birds, then the flocks
        const flockSnapshot = await firestore.collection("flocks").where("userId", "==", uid).get();
        for (const flockDoc of flockSnapshot.docs) {
            // Delete birds belonging to this flock
            await deleteCollectionRecursive("birds", "flockId", flockDoc.id);
            // Then delete the flock itself (recursive to get any flock subs)
            await firestore.recursiveDelete(flockDoc.ref);
        }

        // 5. Best Effort / Optional
        await deleteCollectionRecursive("flocklets", "userId").catch(() => { });

        // 4. AUTH DELETION
        await admin.auth().deleteUser(uid);

        // 5. AUDIT LOG
        await firestore.collection("auditLogs").add({
            action: "account_deleted",
            targetUid: uid,
            performedBy: uid, // Self-deletion
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            details: "User requested deletion via app (GDPR)."
        });

        console.log(`Account deletion complete for ${uid}`);
        return { success: true };

    } catch (error) {
        console.error("Account deletion failed:", error);
        throw new functions.https.HttpsError("internal", "Failed to delete account. Please contact support.");
    }
});
