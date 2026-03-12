import * as admin from 'firebase-admin';

/**
 * ROLE REGISTRY: The single source of truth for roles and permissions.
 * Add new roles or feature permissions here.
 */
const ROLES_REGISTRY = {
    developer: {
        name: "Developer",
        description: "Full system access, including paywall bypass and system-level configuration.",
        isSystemRole: true,
        permissions: {
            manageBreeds: true,
            manageIncubations: true,
            manageBreedingPlans: true,
            viewAnalytics: true,
            accessPremiumFeatures: true,
            manageUsers: true,
            bypassPaywalls: true,
            disableAds: true
        }
    },
    admin: {
        name: "Administrator",
        description: "Standard administrative access. Manage users and data, but subject to billing rules.",
        isSystemRole: true,
        permissions: {
            manageBreeds: true,
            manageIncubations: true,
            manageBreedingPlans: true,
            viewAnalytics: true,
            accessPremiumFeatures: true,
            manageUsers: true,
            disableAds: true
        }
    },
    breeder: {
        name: "Breeder",
        description: "Premium user with access to breeding plans and advanced analytics.",
        isSystemRole: true,
        permissions: {
            manageBreeds: true,
            manageIncubations: true,
            manageBreedingPlans: true,
            viewAnalytics: true,
            accessPremiumFeatures: true,
            disableAds: true
        }
    },
    free_user: {
        name: "Standard User",
        description: "Core access only. No premium or administrative privileges.",
        isSystemRole: true,
        permissions: {
            manageIncubations: true,
            manageBreeds: true
        }
    }
} as const;

// --- Derived Types (No need to touch these) ---
type RoleId = keyof typeof ROLES_REGISTRY;
type AppPermission = keyof typeof ROLES_REGISTRY.developer.permissions;

interface Role {
    id: string;
    name: string;
    description: string;
    permissions: Partial<Record<AppPermission, boolean>>;
    isSystemRole: boolean;
    lastUpdated: number;
}


// Admin SDK initialized by caller

export async function seedRolesAndPermissions(db: admin.firestore.Firestore, isDryRun: boolean, verbose: boolean = false): Promise<void> {
    if (verbose) console.log("🚀 Starting Roles and Permissions Seeding...");
    const batch = db.batch();
    let createdCount = 0;
    let skippedCount = 0;

    try {
        for (const [id, data] of Object.entries(ROLES_REGISTRY)) {
            const docRef = db.collection('roles').doc(id);
            const snapshot = await docRef.get();

            if (!snapshot.exists) {
                // Document does not exist, safe to create
                if (!isDryRun) {
                    const role: Role = {
                        id,
                        ...data,
                        lastUpdated: Date.now()
                    };
                    batch.set(docRef, role);
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
                console.log(`\n✅ Finished! ${createdCount} roles added, ${skippedCount} roles skipped.`);
            } else {
                console.log(`\n🧪 Dry run complete. ${createdCount} roles would have been added.`);
            }
        } else {
            if (verbose) console.log(`\nℹ️ No new roles to add. ${skippedCount} existing roles found.`);
        }
    } catch (error) {
        console.error("❌ Error during seeding:", error);
        throw error;
    }
}
