import { validateBreedStandardsDataV2 } from './seedBreedStandards';

console.log("Running validation test...");
try {
    validateBreedStandardsDataV2();
    console.log("Validation SUCCEEDED");
    process.exit(0);
} catch (e: any) {
    console.error("Validation FAILED:", e.message);
    process.exit(1);
}
