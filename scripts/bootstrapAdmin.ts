import * as admin from "firebase-admin";

/**
 * BOOTSTRAP SCRIPT
 * 
 * Instructions:
 * 1. Download your serviceAccountKey.json from Firebase Console.
 * 2. Place it in this directory.
 * 3. Update the TARGET_UID below.
 * 4. Run: npx ts-node bootstrapAdmin.ts
 */

const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const TARGET_UID = process.argv[2] ?? "REPLACE_WITH_YOUR_UID";

async function bootstrap() {
    if (!TARGET_UID || TARGET_UID === "REPLACE_WITH_YOUR_UID") {
        console.error("Usage: npx ts-node bootstrapAdmin.ts <uid>");
        process.exit(1);
    }

    console.log(`Bypassing security to grant Admin privileges to: ${TARGET_UID}...`);

    try {
        // 1. Update Firestore Profile
        await admin.firestore().collection("users").doc(TARGET_UID).set({
            isSystemAdmin: true,
            isDeveloper: true,
            isCommunityAdmin: true,
            roles: ["admin", "developer", "community_admin"],
            lastUpdated: Date.now()
        }, { merge: true });

        // 2. Grant Custom Claim
        await admin.auth().setCustomUserClaims(TARGET_UID, {
            isAdmin: true,
            isSystemAdmin: true,
            isDeveloper: true,
            isCommunityAdmin: true
        });

        console.log("SUCCESS: User profile + custom claims updated.");
        console.log("Next: Force token refresh (sign out/in) so claims are picked up by the app.");
        process.exit(0);
    } catch (error) {
        console.error("BOOTSTRAP FAILED:", error);
        process.exit(1);
    }
}

bootstrap();
