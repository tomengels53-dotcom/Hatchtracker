import * as admin from 'firebase-admin';

// Initialize Admin SDK
const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

interface Permission {
    id: string;
    description: string;
    allowedRoles: string[]; // ["admin", "moderator"]
}

const PERMISSIONS: Permission[] = [
    {
        id: "delete_post",
        description: "Ability to delete invalid or offensive posts.",
        allowedRoles: ["admin", "moderator"]
    },
    {
        id: "delete_comment",
        description: "Ability to delete invalid or offensive comments.",
        allowedRoles: ["admin", "moderator"]
    },
    {
        id: "manage_reports",
        description: "Ability to view and act on user reports.",
        allowedRoles: ["admin", "moderator"]
    },
    {
        id: "ban_user",
        description: "Ability to ban a user from the platform.",
        allowedRoles: ["admin"]
    },
    {
        id: "pin_post",
        description: "Ability to pin a post to the top of the feed.",
        allowedRoles: ["admin", "moderator"]
    }
];

async function seedPermissions() {
    console.log("🚀 Seeding Permissions...");
    const batch = db.batch();

    for (const perm of PERMISSIONS) {
        const ref = db.collection('permissions').doc(perm.id);
        batch.set(ref, perm, { merge: true });
        console.log(`Prepared: ${perm.id}`);
    }

    await batch.commit();
    console.log("✅ Permissions synced successfully.");
    process.exit(0);
}

seedPermissions().catch(console.error);
