import * as admin from 'firebase-admin';

// Initialize Admin SDK
const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

interface BillingProduct {
    productId: string;
    subscriptionTier: "expert" | "pro";
    billingPeriod: "monthly" | "yearly";
    source: "google_play";
    active: boolean;
}

const BILLING_PRODUCTS: BillingProduct[] = [
    {
        productId: "subscription_expert_monthly",
        subscriptionTier: "expert",
        billingPeriod: "monthly",
        source: "google_play",
        active: true
    },
    {
        productId: "subscription_expert_yearly",
        subscriptionTier: "expert",
        billingPeriod: "yearly",
        source: "google_play",
        active: true
    },
    {
        productId: "subscription_pro_monthly",
        subscriptionTier: "pro",
        billingPeriod: "monthly",
        source: "google_play",
        active: true
    },
    {
        productId: "subscription_pro_yearly",
        subscriptionTier: "pro",
        billingPeriod: "yearly",
        source: "google_play",
        active: true
    }
];

async function seedBillingProducts() {
    console.log("🚀 Seeding Billing Products...");
    const batch = db.batch();

    for (const product of BILLING_PRODUCTS) {
        // Use productId as document ID for O(1) lookups
        const ref = db.collection('billingProducts').doc(product.productId);
        batch.set(ref, product, { merge: true });
        console.log(`Prepared: ${product.productId}`);
    }

    await batch.commit();
    console.log("✅ Billing products synced successfully.");
    process.exit(0);
}

seedBillingProducts().catch(console.error);
