import * as admin from 'firebase-admin';

// Initialize Admin SDK
const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

interface FeatureFlag {
    id: string;
    description: string;
    enabled: boolean;
    tiersAllowed: string[]; // ["free", "expert", "pro"]
}

const FEATURE_FLAGS: FeatureFlag[] = [
    {
        id: "selective_breeding",
        description: "Access to advanced genetic algorithm pairing tools.",
        enabled: true,
        tiersAllowed: ["pro"]
    },
    {
        id: "export_csv",
        description: "Ability to export flock data to CSV/Excel.",
        enabled: true,
        tiersAllowed: ["expert", "pro"]
    },
    {
        id: "cloud_backup",
        description: "Automatic cloud sync of local data.",
        enabled: true,
        tiersAllowed: ["free", "expert", "pro"] // Available to all, but maybe limited quota
    },
    {
        id: "beta_family_tree",
        description: "New visual family tree explorer (BETA).",
        enabled: false, // Disabled globally for now
        tiersAllowed: ["pro"]
    }
];

async function seedFeatureFlags() {
    console.log("🚀 Seeding Feature Flags...");
    const batch = db.batch();

    for (const flag of FEATURE_FLAGS) {
        const ref = db.collection('featureFlags').doc(flag.id);
        batch.set(ref, flag, { merge: true });
        console.log(`Prepared: ${flag.id}`);
    }

    await batch.commit();
    console.log("✅ Feature flags synced successfully.");
    process.exit(0);
}

seedFeatureFlags().catch(console.error);
