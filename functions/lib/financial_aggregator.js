"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onFinancialEntryWrite = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const firestore = admin.firestore();
/**
 * Triggered on any write to financialEntries.
 * Recalculates total costs, revenue, and unit-based metrics for the parent entity.
 *
 * Contract (Parity with Kotlin FinancialCalculationService):
 * - Profit = RevenueGross - CostGross
 * - Unit Metric = CostNet / unitCount
 */
exports.onFinancialEntryWrite = functions.firestore
    .document("users/{userId}/financialEntries/{entryId}")
    .onWrite(async (change, context) => {
    var _a, _b;
    const userId = context.params.userId;
    const entryData = change.after.exists ? change.after.data() : change.before.data();
    if (!entryData)
        return null;
    const { ownerType, ownerId } = entryData;
    // 1. Fetch all entries for this owner to ensure absolute accuracy (Audit Trail)
    const entriesSnapshot = await firestore
        .collection(`users/${userId}/financialEntries`)
        .where("ownerType", "==", ownerType)
        .where("ownerId", "==", ownerId)
        .get();
    let totalCostsGross = 0;
    let totalCostsNet = 0;
    let totalRevenueGross = 0;
    entriesSnapshot.forEach((doc) => {
        const data = doc.data();
        const vatEnabled = data.vatEnabled || false;
        const amount = data.amount || 0;
        const amountGross = vatEnabled ? (data.amountGross || amount) : amount;
        const amountNet = vatEnabled ? (data.amountNet || amount) : amount;
        if (data.type === "cost") {
            totalCostsGross += amountGross;
            totalCostsNet += amountNet;
        }
        else if (data.type === "revenue") {
            totalRevenueGross += amountGross;
        }
    });
    const profit = totalRevenueGross - totalCostsGross;
    // 2. Retrieve Counts for Advanced Metrics
    let unitCount = 0;
    let metricField = "";
    try {
        if (ownerType === "flocklet") {
            const doc = await firestore.collection("flocklets").doc(ownerId).get();
            unitCount = ((_a = doc.data()) === null || _a === void 0 ? void 0 : _a.chickCount) || 0;
            metricField = "costPerChick";
        }
        else if (ownerType === "incubation") {
            const doc = await firestore.collection("incubations").doc(ownerId).get();
            unitCount = ((_b = doc.data()) === null || _b === void 0 ? void 0 : _b.eggsCount) || 0;
            metricField = "costPerEgg";
        }
        else if (ownerType === "flock") {
            // Count birds in this flock
            const snapshot = await firestore
                .collection("birds")
                .where("flockId", "==", ownerId)
                .get();
            unitCount = snapshot.size;
            metricField = "costPerAdult";
        }
    }
    catch (error) {
        console.error(`Error fetching unit counts for ${ownerType} ${ownerId}:`, error);
    }
    // 3. Calculate Advanced Metrics (VAT excluded from unit cost)
    const unitMetric = unitCount > 0 ? totalCostsNet / unitCount : 0;
    // 4. Update Summary
    const summaryRef = firestore.collection(`users/${userId}/financialSummaries`).doc(`${ownerType}_${ownerId}`);
    const summaryUpdate = {
        ownerType,
        ownerId,
        totalCosts: totalCostsGross,
        totalRevenue: totalRevenueGross,
        profit,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };
    if (metricField) {
        summaryUpdate[metricField] = unitMetric;
    }
    return summaryRef.set(summaryUpdate, { merge: true });
});
//# sourceMappingURL=financial_aggregator.js.map