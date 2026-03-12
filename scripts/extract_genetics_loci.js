const chickens = require('./data_genetics_chickens.js');
const other = require('./data_genetics_other.js');

const allData = { ...chickens, ...other };
const loci = new Set();

for (const breedId in allData) {
    const baseline = allData[breedId].baselineGenotype || {};
    for (const key in baseline) {
        if (key !== "SPECIES_Specific") {
            loci.add(key);
        }
    }
}

console.log([...loci].sort().join('\n'));
