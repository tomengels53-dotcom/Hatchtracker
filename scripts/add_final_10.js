const fs = require('fs');
const path = require('path');

const ktPath = path.join(__dirname, '../core/data/src/main/java/com/example/hatchtracker/data/repository/BreedStandardRepository.kt');
const tsPath = path.join(__dirname, 'seedBreedStandards.ts');

const chickens = require('./data_genetics_chickens.js');
const other = require('./data_genetics_other.js');
const allGeneticData = { ...chickens, ...other };

const missingIds = [
    "belgian_danvers", "barbu_de_boitsfort", "brakel", "netherlands_owlbeard",
    "vorwerk", "westfalischer_totleger", "czech_gold_brakel", "danish_landrace",
    "drentse_eend", "pomeranian_goose"
];

const ktContent = fs.readFileSync(ktPath, 'utf8');
let tsContent = fs.readFileSync(tsPath, 'utf8');

// Parse specifically for these 10 breeds
// We use a regex that captures the whole BreedStandard block without relying on combType
// Iterate all BreedStandard blocks and check ID
const breedRegex = /BreedStandard\([\s\S]+?\)/g;
const breedsToAdd = [];

let match;
while ((match = breedRegex.exec(ktContent)) !== null) {
    const block = match[0];
    const idMatch = block.match(/id\s*=\s*"([^"]+)"/);
    if (!idMatch) continue;

    const id = idMatch[1];
    if (missingIds.includes(id)) {
        // Extract fields
        const name = (block.match(/name\s*=\s*"([^"]+)"/) || [])[1];
        const origin = (block.match(/origin\s*=\s*"([^"]+)"/) || [])[1];
        const species = (block.match(/species\s*=\s*"([^"]+)"/) || [])[1];
        const eggColor = (block.match(/eggColor\s*=\s*"([^"]+)"/) || [])[1];

        const acceptedColorsMatch = block.match(/acceptedColors\s*=\s*listOf\(([^)]+)\)/);
        const acceptedColors = acceptedColorsMatch ? acceptedColorsMatch[1].split(',').map(s => s.trim().replace(/"/g, '')) : ["Standard"];

        const wRooster = (block.match(/weightRoosterKg\s*=\s*([0-9.]+)/) || [])[1];
        const wHen = (block.match(/weightHenKg\s*=\s*([0-9.]+)/) || [])[1];

        const official = (block.match(/official\s*=\s*(true|false)/) || [])[1];

        const recognizedByMatch = block.match(/recognizedBy\s*=\s*listOf\(([^)]*)\)/);
        const recognizedBy = recognizedByMatch ? recognizedByMatch[1].split(',').map(s => s.trim().replace(/"/g, '')) : [];

        const category = (block.match(/category\s*=\s*"([^"]+)"/) || [])[1];
        const isTrueBantam = (block.match(/isTrueBantam\s*=\s*(true|false)/) || [])[1];

        breedsToAdd.push({
            id, name, origin, species, eggColor, acceptedColors,
            weightRoosterKg: parseFloat(wRooster || "2.0"), weightHenKg: parseFloat(wHen || "1.5"),
            official: official === 'true', recognizedBy, category, isTrueBantam: isTrueBantam === 'true'
        });
    }
}

console.log(`Found data for ${breedsToAdd.length} missing breeds in KT.`);

if (breedsToAdd.length === 0) {
    console.log("No breeds found???");
    process.exit(0);
}

let newEntries = "\n    // --- FINAL MISSING BREEDS ---\n";

for (const breed of breedsToAdd) {
    const geneticData = allGeneticData[breed.id] || {
        baselineGenotype: { "O_Locus": "o/o" },
        knownGenes: [], fixedTraits: [], inferredTraits: [], unknownTraits: [], confidenceLevel: "low"
    };

    let entry = "    {\n";
    entry += `        id: "${breed.id}",\n`;
    entry += `        name: "${breed.name}",\n`;
    entry += `        origin: "${breed.origin}",\n`;
    entry += `        species: "${breed.species}",\n`;
    entry += `        eggColor: "${breed.eggColor}",\n`;
    entry += `        acceptedColors: ${JSON.stringify(breed.acceptedColors)},\n`;
    entry += `        weightRoosterKg: ${breed.weightRoosterKg},\n`;
    entry += `        weightHenKg: ${breed.weightHenKg},\n`;

    // Determine size/class defaults
    const size = breed.weightHenKg < 1.0 ? "Small" : (breed.weightHenKg > 3.0 ? "Large" : "Medium");
    const weightClass = breed.weightHenKg < 1.0 ? "Bantam" : (breed.weightHenKg > 3.5 ? "Heavy" : (breed.weightHenKg < 2.5 ? "Light" : "Medium"));

    entry += `        size: "${size}",\n`;
    entry += `        weightClass: "${weightClass}",\n`;
    entry += `        eggProduction: 150,\n`;
    entry += `        official: ${breed.official},\n`;
    entry += `        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),\n`;
    entry += `        recognizedBy: ${JSON.stringify(breed.recognizedBy)},\n`;

    if (breed.category) entry += `        category: "${breed.category}",\n`;
    if (breed.isTrueBantam) entry += `        isTrueBantam: ${breed.isTrueBantam},\n`;

    entry += `        geneticProfile: {\n`;
    entry += `            knownGenes: ${JSON.stringify(geneticData.knownGenes || [])},\n`;

    let bgStr = "{\n";
    const bgKeys = Object.keys(geneticData.baselineGenotype || {});
    if (bgKeys.length > 0) {
        for (const k of bgKeys) {
            bgStr += `                ${k}: "${geneticData.baselineGenotype[k]}",\n`;
        }
        bgStr += "            }";
    } else {
        bgStr = "{}";
    }

    entry += `            baselineGenotype: ${bgStr},\n`;
    entry += `            fixedTraits: ${JSON.stringify(geneticData.fixedTraits || [])},\n`;

    if (geneticData.inferredTraits && geneticData.inferredTraits.length)
        entry += `            inferredTraits: ${JSON.stringify(geneticData.inferredTraits)},\n`;
    if (geneticData.unknownTraits && geneticData.unknownTraits.length)
        entry += `            unknownTraits: ${JSON.stringify(geneticData.unknownTraits)},\n`;

    entry += `            confidenceLevel: "${geneticData.confidenceLevel || 'medium'}"\n`;
    entry += `        }\n`;
    entry += "    },";

    newEntries += entry + "\n";
}

const lastBracket = tsContent.lastIndexOf('];');
if (lastBracket === -1) {
    console.error("Could not find closing bracket!");
    process.exit(1);
}

tsContent = tsContent.substring(0, lastBracket) + newEntries + tsContent.substring(lastBracket);

fs.writeFileSync(tsPath, tsContent);
console.log(`Successfully appended ${breedsToAdd.length} final missing breeds.`);
