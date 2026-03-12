const fs = require('fs');
const path = require('path');

const ktPath = path.join(__dirname, '../core/data/src/main/java/com/example/hatchtracker/data/repository/BreedStandardRepository.kt');
const tsPath = path.join(__dirname, 'seedBreedStandards.ts');

const ktContent = fs.readFileSync(ktPath, 'utf8');
const tsContent = fs.readFileSync(tsPath, 'utf8');

const ktIds = new Set();
let match;
const ktRegex = /id\s*=\s*"([^"]+)"/g;
while ((match = ktRegex.exec(ktContent)) !== null) {
    ktIds.add(match[1]);
}

const tsIds = new Set();
const tsRegex = /id:\s*"([^"]+)"/g;
while ((match = tsRegex.exec(tsContent)) !== null) {
    tsIds.add(match[1]);
}

console.log(`KT has ${ktIds.size} IDs.`);
console.log(`TS has ${tsIds.size} IDs.`);

const missingInTS = [...ktIds].filter(id => !tsIds.has(id));
console.log(`Missing in TS: ${missingInTS.length}`);
if (missingInTS.length > 0) {
    console.log(missingInTS.join(', '));
}
