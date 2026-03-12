import { validateBreedStandardsData } from './seedBreedStandards';

try {
    validateBreedStandardsData();
} catch (error) {
    if (error instanceof Error) {
        console.error(error.message);
    } else {
        console.error(String(error));
    }
    process.exit(1);
}
