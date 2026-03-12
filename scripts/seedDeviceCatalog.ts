import * as admin from 'firebase-admin';

export interface DeviceCatalogItem {
    id: string; // e.g., 'brinsea_mini_ii_advance'
    brand: string;
    model: string;
    type: "incubator" | "hatcher" | "combo";
    capacityEggs: number; // Approximate chicken egg capacity
    features: {
        autoTurning: boolean;
        tempControl: boolean;
        humidityControl: boolean; // Integrated pump/humidity control
        cooling: boolean; // Periodic cooling
    };
    imageUrl?: string;
    websiteUrl?: string;
}

const deviceCatalog: DeviceCatalogItem[] = [
    {
        id: "brinsea_mini_ii_advance",
        brand: "Brinsea",
        model: "Mini II Advance",
        type: "incubator",
        capacityEggs: 7,
        features: {
            autoTurning: true,
            tempControl: true,
            humidityControl: false,
            cooling: true
        }
    },
    {
        id: "brinsea_ovation_28_ex",
        brand: "Brinsea",
        model: "Ovation 28 EX",
        type: "incubator",
        capacityEggs: 28,
        features: {
            autoTurning: true,
            tempControl: true,
            humidityControl: true,
            cooling: true
        }
    },
    {
        id: "rcom_king_suro_20",
        brand: "Rcom",
        model: "King Suro 20",
        type: "incubator",
        capacityEggs: 24,
        features: {
            autoTurning: true,
            tempControl: true,
            humidityControl: true,
            cooling: false
        }
    },
    {
        id: "generic_manual_incubator",
        brand: "Generic",
        model: "Manual Incubator",
        type: "incubator",
        capacityEggs: 48,
        features: {
            autoTurning: false,
            tempControl: true,
            humidityControl: false,
            cooling: false
        }
    },
    {
        id: "generic_hatcher",
        brand: "Generic",
        model: "Hatcher Box",
        type: "hatcher",
        capacityEggs: 50,
        features: {
            autoTurning: false,
            tempControl: true,
            humidityControl: false,
            cooling: false
        }
    }
];

export async function seedDeviceCatalog(db: admin.firestore.Firestore, isDryRun: boolean, verbose: boolean = false): Promise<void> {
    if (verbose) console.log("📦 starting Device Catalog seed...");
    const batch = db.batch();
    let count = 0;

    for (const device of deviceCatalog) {
        const docRef = db.collection('deviceCatalog').doc(device.id);

        if (!isDryRun) {
            // Use set w/ merge to allow user overrides but enforce defaults on known IDs?
            // Actually, for catalog, we probably want to enforce our source of truth.
            batch.set(docRef, {
                ...device,
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });
        }

        if (verbose) console.log(`${isDryRun ? '[DRY RUN] ' : ''}Upserting device: ${device.id}`);
        count++;
    }

    if (!isDryRun) {
        await batch.commit();
        if (verbose) console.log(`✅ Committed ${count} device catalog entries.`);
    } else {
        if (verbose) console.log(`ℹ️ [DRY RUN] Would have committed ${count} device catalog entries.`);
    }
}
