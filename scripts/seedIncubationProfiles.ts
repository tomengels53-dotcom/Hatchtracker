import * as admin from 'firebase-admin';

/**
 * Single source of truth for incubation biology.
 */
interface IncubationProfile {
    species: string;
    incubationDurationDays: number;

    // Temperature in °C
    temperature: {
        min: number;
        optimal: number;
        max: number;
    };

    // Humidity in %
    humidity: {
        earlyPhase: { min: number; max: number };
        lockdownPhase: { min: number; max: number };
    };

    lockdownStartDay: number;

    turning: {
        required: boolean;
        timesPerDay: number;
        stopDay: number;
    };

    ventilationRequirement: "low" | "medium" | "high";
    notes?: string;
}

// Admin SDK initialized by caller

const incubationProfiles: IncubationProfile[] = [
    {
        species: "Chicken",
        incubationDurationDays: 21,
        temperature: { min: 37.0, optimal: 37.5, max: 38.2 },
        humidity: {
            earlyPhase: { min: 45, max: 55 },
            lockdownPhase: { min: 65, max: 75 }
        },
        lockdownStartDay: 18,
        turning: {
            required: true,
            timesPerDay: 5,
            stopDay: 18
        },
        ventilationRequirement: "medium",
        notes: "Standard poultry incubation data."
    },
    {
        species: "Duck",
        incubationDurationDays: 28,
        temperature: { min: 37.0, optimal: 37.5, max: 37.8 },
        humidity: {
            earlyPhase: { min: 50, max: 60 },
            lockdownPhase: { min: 75, max: 85 }
        },
        lockdownStartDay: 25,
        turning: {
            required: true,
            timesPerDay: 4,
            stopDay: 25
        },
        ventilationRequirement: "high",
        notes: "Requires higher humidity than chickens. Muscovy ducks require 35 days."
    },
    {
        species: "Goose",
        incubationDurationDays: 30,
        temperature: { min: 37.0, optimal: 37.2, max: 37.5 },
        humidity: {
            earlyPhase: { min: 50, max: 60 },
            lockdownPhase: { min: 75, max: 85 }
        },
        lockdownStartDay: 27,
        turning: {
            required: true,
            timesPerDay: 4,
            stopDay: 27
        },
        ventilationRequirement: "high",
        notes: "Daily misting/cooling often recommended after day 7."
    },
    {
        species: "Turkey",
        incubationDurationDays: 28,
        temperature: { min: 37.0, optimal: 37.5, max: 38.0 },
        humidity: {
            earlyPhase: { min: 50, max: 55 },
            lockdownPhase: { min: 70, max: 75 }
        },
        lockdownStartDay: 25,
        turning: {
            required: true,
            timesPerDay: 5,
            stopDay: 25
        },
        ventilationRequirement: "medium"
    },
    {
        species: "Peafowl",
        incubationDurationDays: 28,
        temperature: { min: 37.0, optimal: 37.5, max: 37.8 },
        humidity: {
            earlyPhase: { min: 50, max: 55 },
            lockdownPhase: { min: 70, max: 75 }
        },
        lockdownStartDay: 25,
        turning: {
            required: true,
            timesPerDay: 5,
            stopDay: 25
        },
        ventilationRequirement: "medium"
    }
];

export async function seedIncubationProfiles(db: admin.firestore.Firestore, isDryRun: boolean, verbose: boolean = false): Promise<void> {
    if (verbose) console.log("🐣 Starting incubation profiles seed...");
    const collectionRef = db.collection('incubationProfiles');
    const batch = db.batch();
    let count = 0;

    for (const profile of incubationProfiles) {
        const docRef = collectionRef.doc(profile.species.toLowerCase());
        if (!isDryRun) {
            batch.set(docRef, {
                ...profile,
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true }); // Use merge to allow for potential updates if needed
        }
        if (verbose) console.log(`${isDryRun ? '[DRY RUN] ' : ''}Upserting profile for ${profile.species}...`);
        count++;
    }

    if (!isDryRun) {
        await batch.commit();
        if (verbose) console.log(`✅ Committed ${count} incubation profiles.`);
    } else {
        if (verbose) console.log(`ℹ️ [DRY RUN] Would have committed ${count} incubation profiles.`);
    }
}
