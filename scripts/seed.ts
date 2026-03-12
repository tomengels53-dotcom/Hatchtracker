
import * as admin from 'firebase-admin';
import * as path from 'path';

// Import individual seeders
import { seedBreedStandards } from './seedBreedStandards';
import { seedIncubationProfiles } from './seedIncubationProfiles';
import { seedSubscriptionTiers } from './seedSubscriptionTiers';
import { seedRolesAndPermissions } from './seedRolesAndPermissions';
import { seedDeviceCatalog } from './seedDeviceCatalog';


// --- Configuration ---
const SERVICE_ACCOUNT_PATH = './serviceAccountKey.json';
const CURRENT_SEED_VERSION = 1; // Increment this when logic changes

// --- Argument Parsing ---
const args = process.argv.slice(2);
const isDryRun = args.includes('--dry-run');
const verbose = args.includes('--verbose') || isDryRun;
const forceInfo = args.includes('--force');
// naive parser for --only
const onlyIndex = args.indexOf('--only');
const onlySeeder = onlyIndex !== -1 ? args[onlyIndex + 1] : null;

async function main() {
    console.log(`\n🥚 HatchTracker Seeding Pipeline (v${CURRENT_SEED_VERSION}) 🥚`);
    console.log(`==================================`);
    console.log(`Mode: ${isDryRun ? '🧪 DRY RUN' : '🚀 LIVE EXECUTION'}`);
    console.log(`Verbose: ${verbose}`);
    if (onlySeeder) console.log(`Filter: Running only '${onlySeeder}'`);
    if (forceInfo) console.log(`Force: Enabled (Ignoring version checks)`);
    console.log(`----------------------------------\n`);

    // 1. Initialize Firebase Admin
    try {
        const serviceAccount = require(path.resolve(__dirname, SERVICE_ACCOUNT_PATH));
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
        });
        console.log("✅ Firebase Admin initialized.");
    } catch (error) {
        console.error("❌ Failed to initialize Firebase Admin. check serviceAccountKey.json");
        console.error(error);
        process.exit(1);
    }

    const db = admin.firestore();

    // Check version
    const versionRef = db.collection('system').doc('seedVersion');
    if (!forceInfo && !onlySeeder) {
        const doc = await versionRef.get();
        if (doc.exists) {
            const data = doc.data();
            if (data && data.version >= CURRENT_SEED_VERSION) {
                console.log(`ℹ️ System already at version ${data.version}. Use --force to re-run.`);
                if (!isDryRun) return;
            }
        }
    }

    // 2. Run Seeders
    try {
        const shouldRun = (name: string) => !onlySeeder || onlySeeder === name;

        // Breed Standards
        if (shouldRun('breeds')) {
            await seedBreedStandards(db, isDryRun, verbose);
            console.log("");
        }

        // Incubation Profiles
        if (shouldRun('profiles')) {
            await seedIncubationProfiles(db, isDryRun, verbose);
            console.log("");
        }

        // Subscription Tiers
        if (shouldRun('tiers')) {
            await seedSubscriptionTiers(db, isDryRun, verbose);
            console.log("");
        }

        // Roles & Permissions
        if (shouldRun('roles')) {
            await seedRolesAndPermissions(db, isDryRun, verbose);
            console.log("");
        }

        // Device Catalog
        if (shouldRun('devices')) {
            await seedDeviceCatalog(db, isDryRun, verbose);
            console.log("");
        }

        console.log("----------------------------------");
        if (isDryRun) {
            console.log("🧪 Seeding pipeline completed in DRY RUN mode. No changes made.");
        } else {
            // Update version
            if (!onlySeeder) {
                await versionRef.set({
                    version: CURRENT_SEED_VERSION,
                    lastRun: admin.firestore.FieldValue.serverTimestamp(),
                    updatedBy: 'seed-script'
                });
                console.log(`✅ System version updated to ${CURRENT_SEED_VERSION}.`);
            }
            console.log("✅ Seeding pipeline completed successfully.");
        }

    } catch (error) {
        console.error("\n❌ Seeding failed!");
        console.error(error);
        process.exit(1);
    }
}

main().catch(console.error);
