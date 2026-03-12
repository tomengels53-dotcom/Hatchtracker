const fs = require('fs');
const path = require('path');
const content = fs.readFileSync(path.join(__dirname, 'seedBreedStandards.ts'), 'utf8');

const regex = /baselineGenotype:\s*\{([^}]+)\}/g;
let match;
const loci = new Set();
let count = 0;

while ((match = regex.exec(content)) !== null) {
    const block = match[1];
    const lines = block.split('\n');
    lines.forEach(line => {
        const parts = line.split(':');
        if (parts.length > 1) {
            let key = parts[0].trim().replace(/^"|"$/g, ''); // remove quotes
            // remove trailing comma from key if accidentally captured (regex shouldn't capture key only)
            // split by : gives key on left
            if (key) {
                // remove quotes if any again just in case
                key = key.replace(/['"]/g, '');
                loci.add(key);
                count++;
            }
        }
    });
}
console.log(`Found ${count} total genotypes.`);
console.log([...loci].sort().join('\n'));
