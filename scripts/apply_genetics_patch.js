const fs = require('fs');
const path = require('path');

const tsPath = path.join(__dirname, 'seedBreedStandards.ts');
let tsContent = fs.readFileSync(tsPath, 'utf8');

const chickens = require('./data_genetics_chickens.js');
const other = require('./data_genetics_other.js');
const allData = { ...chickens, ...other };

let patchedCount = 0;
let notFoundCount = 0;

console.log(`Starting patch for ${Object.keys(allData).length} breeds...`);

for (const breedId in allData) {
    const data = allData[breedId];

    // Find valid ID match that isn't commented out
    const idRegex = new RegExp(`id:\\s*"${breedId}"`);
    const idMatch = tsContent.match(idRegex);

    if (!idMatch) {
        console.log(`Breed ID not found in file: ${breedId}`);
        notFoundCount++;
        continue;
    }

    // Search forward for geneticProfile
    const searchStartIndex = idMatch.index;
    const geneticProfileMatch = tsContent.substring(searchStartIndex).match(/geneticProfile:\s*\{/);

    if (!geneticProfileMatch) {
        console.log(`No geneticProfile block found for ${breedId}`);
        continue;
    }

    const absoluteStart = searchStartIndex + geneticProfileMatch.index;

    // Find the end of the block by counting braces
    let braceCount = 0;
    let endIndex = -1;
    const openBraceIndex = tsContent.indexOf('{', absoluteStart);

    if (openBraceIndex === -1) continue;

    for (let i = openBraceIndex; i < tsContent.length; i++) {
        if (tsContent[i] === '{') braceCount++;
        if (tsContent[i] === '}') braceCount--;

        if (braceCount === 0) {
            endIndex = i + 1;
            break;
        }
    }

    if (endIndex === -1) {
        console.log(`Could not find closing brace for ${breedId}`);
        continue;
    }

    // Construct new geneticProfile content
    const newProfile = {
        knownGenes: data.knownGenes || [],
        baselineGenotype: data.baselineGenotype || {},
        fixedTraits: data.fixedTraits || [],
        inferredTraits: data.inferredTraits || [],
        unknownTraits: data.unknownTraits || [],
        confidenceLevel: data.confidenceLevel || "medium"
    };

    let profileStr = "geneticProfile: {\n";
    profileStr += `            knownGenes: ${JSON.stringify(newProfile.knownGenes)},\n`;

    let bgStr = "{\n";
    const bgKeys = Object.keys(newProfile.baselineGenotype);
    if (bgKeys.length > 0) {
        for (const k of bgKeys) {
            bgStr += `                ${k}: "${newProfile.baselineGenotype[k]}",\n`;
        }
        bgStr += "            }";
    } else {
        bgStr = "{}";
    }

    profileStr += `            baselineGenotype: ${bgStr},\n`;
    profileStr += `            fixedTraits: ${JSON.stringify(newProfile.fixedTraits)},\n`;

    if (newProfile.inferredTraits && newProfile.inferredTraits.length > 0)
        profileStr += `            inferredTraits: ${JSON.stringify(newProfile.inferredTraits)},\n`;

    if (newProfile.unknownTraits && newProfile.unknownTraits.length > 0)
        profileStr += `            unknownTraits: ${JSON.stringify(newProfile.unknownTraits)},\n`;

    profileStr += `            confidenceLevel: "${newProfile.confidenceLevel}"\n`;
    profileStr += "        }";

    // Replace
    const before = tsContent.substring(0, absoluteStart);
    const after = tsContent.substring(endIndex);
    tsContent = before + profileStr + after;

    patchedCount++;
}

fs.writeFileSync(tsPath, tsContent);
console.log(`Successfully patched ${patchedCount} breeds. IDs not found: ${notFoundCount}`);
