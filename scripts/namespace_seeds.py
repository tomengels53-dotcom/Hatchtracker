import re

file_path = r'c:\Users\Tom\AndroidStudioProjects\HatchTracker\scripts\seedBreedStandards.ts'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

def namespace_genotypes(match):
    breed_block = match.group(0)
    
    # Extract species
    species_match = re.search(r'species:\s*"(.*?)"', breed_block)
    if not species_match:
        return breed_block
    
    species = species_match.group(1)
    if species == "Chicken":
        return breed_block
    
    prefix = species.upper() + "__"
    
    # Extract baselineGenotype block if it exists
    genotype_match = re.search(r'baselineGenotype:\s*\{(.*?)\}', breed_block, re.DOTALL)
    if not genotype_match:
        return breed_block
    
    genotype_content = genotype_match.group(1)
    
    # Prefix keys that don't already have a prefix
    # Pattern: finds "Key:" and ensures it doesn't already have "__"
    def add_prefix(content_match):
        key = content_match.group(1)
        if "__" in key:
            return content_match.group(0)
        return f"{prefix}{key}"

    new_genotype_content = re.sub(r'(\b\w+Locus\b)', add_prefix, genotype_content)
    
    new_genotype_block = f'baselineGenotype: {{{new_genotype_content}}}'
    return breed_block.replace(genotype_match.group(0), new_genotype_block)

# Pattern matches an entire breed standard object in the array
# We look for { id: ..., species: ..., geneticProfile: { ... baselineGenotype: { ... } } }
# This regex is a bit simplified but should work if the indentation is standard.
pattern = re.compile(r'\{\s*id:\s*".*?".*?\}', re.DOTALL)

new_content = pattern.sub(namespace_genotypes, content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Successfully namespaced non-chicken genotypes.")
