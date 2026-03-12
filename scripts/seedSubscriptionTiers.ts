import * as admin from 'firebase-admin';

/**
 * ROLE REGISTRY: The single source of truth for subscription tiers.
 * Add new tiers or update metadata here.
 */
const SUBSCRIPTION_REGISTRY = {
    free: {
        name: "FREE",
        description: "Basic features for casual breeders. Supported by ads.",
        monthlyPrice: 0,
        yearlyPrice: 0,
        features: [
            "hatchOutcomeTracking",
            "cloudBackup"
        ],
        maxIncubations: 3,
        maxBreedingPlans: 0,
        adsEnabled: true,
        prioritySupport: false,
        ordinal: 0
    },
    expert: {
        name: "EXPERT",
        description: "Advanced tools for enthusiast breeders. Ad-free experience.",
        monthlyPrice: 4.99,
        yearlyPrice: 49.99,
        features: [
            "hatchOutcomeTracking",
            "parentOffspringTracking",
            "advancedAnalytics",
            "cloudBackup"
        ],
        maxIncubations: null, // Unlimited
        maxBreedingPlans: 5,
        adsEnabled: false,
        prioritySupport: false,
        ordinal: 1
    },
    pro: {
        name: "PRO",
        description: "The complete suite for professional hatcheries.",
        monthlyPrice: 9.99,
        yearlyPrice: 99.99,
        features: [
            "hatchOutcomeTracking",
            "parentOffspringTracking",
            "selectiveBreedingLogic",
            "advancedAnalytics",
            "cloudBackup"
        ],
        maxIncubations: null, // Unlimited
        maxBreedingPlans: null, // Unlimited
        adsEnabled: false,
        prioritySupport: true,
        ordinal: 2
    }
} as const;

// --- Derived Types ---
type TierId = keyof typeof SUBSCRIPTION_REGISTRY;

interface SubscriptionTierMetadata {
    id: string;
    name: string;
    description: string;
    monthlyPrice: number;
    yearlyPrice: number;
    features: string[];
    maxIncubations: number | null;
    maxBreedingPlans: number | null;
    adsEnabled: boolean;
    prioritySupport: boolean;
    ordinal: number;
    lastUpdated: number;
}

// Admin SDK initialized by caller

export async function seedSubscriptionTiers(db: admin.firestore.Firestore, isDryRun: boolean, verbose: boolean = false): Promise<void> {
    if (verbose) console.log("🚀 Starting Subscription Tiers Seeding...");
    const batch = db.batch();
    let createdCount = 0;
    let skippedCount = 0;

    try {
        for (const [id, data] of Object.entries(SUBSCRIPTION_REGISTRY)) {
            const docRef = db.collection('subscriptionTiers').doc(id);
            const snapshot = await docRef.get();

            if (!snapshot.exists) {
                // Document does not exist, safe to create
                if (!isDryRun) {
                    const tier: SubscriptionTierMetadata = {
                        id,
                        ...data,
                        features: [...data.features], // Clone array
                        lastUpdated: Date.now()
                    };
                    batch.set(docRef, tier);
                }
                console.log(`${isDryRun ? '🔍 [WOULD CREATE]' : '✨ Created'}: ${id}`);
                createdCount++;
            } else {
                // Document exists, skipping to prevent overwrite
                if (verbose) console.log(`⏩ Skipped: ${id} (already exists)`);
                skippedCount++;
            }
        }

        if (createdCount > 0) {
            if (!isDryRun) {
                await batch.commit();
                console.log(`\n✅ Finished! ${createdCount} tiers added, ${skippedCount} tiers skipped.`);
            } else {
                console.log(`\n🧪 Dry run complete. ${createdCount} tiers would have been added.`);
            }
        } else {
            if (verbose) console.log(`\nℹ️ No new tiers to add. ${skippedCount} existing tiers found.`);
        }
    } catch (error) {
        console.error("❌ Error during seeding:", error);
        throw error;
    }
}
