# HatchTracker Genetics Implementation Audit

**Date:** 2026-02-15
**Phase:** 0 (Mandatory Audit)

## Executive Summary
This audit confirms that the current genetics implementation is based on heuristic scoring and hardcoded string matching, which is insufficient for the Pro "Deterministic Genetics" goal. A complete refactor to a Mendelian/Z-linked probability engine is required.

## 1. `PredictOffspringTraits.kt` Analysis
**Status:** HEURISTIC ONLY - TO BE DEPRECATED

- **Logic:** Uses arbitrary probability weights based on data source reliability rather than genetic inheritance rules.
    - `InheritedFrom.BOTH` -> 0.9 / 0.85
    - `InheritedFrom.SIRE/DAM` -> 0.5 / 0.45
    - `InheritedFrom.INFERRED` -> 0.3
- **Mechanism:** Iterates through string lists (`knownGenes`, `fixedTraits`) and assigns confidence tiers.
- **Reference:**
    - Lines 91-96: Hardcoded probability weights.
    - Lines 49-69: String matching logic.

**Action:**
- Deprecate entire file.
- Replace `predictOffspringTraits` function with a facade call to `GeneticsFacade`.
- Map deterministic probabilities back to these legacy `TraitPrediction` objects for UI compatibility during transition.

## 2. `CrossBreedingIntelligenceEngine.kt` Analysis
**Status:** HARDCODED BUSINESS LOGIC - REQUIRES REFACTOR

- **Issue:** Contains explicit breeding rules hardcoded against breed names and string traits. This breaks if a user adds a custom breed or uses a different locale/spelling.
- **Hardcoded References Found:**
    - **Sex-Linked logic:**
        - `male.breed.contains("Rhode Island Red")`
        - `male.breed.contains("New Hampshire")`
        - `female.breed.contains("Plymouth Rock")`
        - `female.breed.contains("Barred")`
    - **Trait logic:**
        - Checks for literal strings: "Blue Egg", "Dark Brown Egg", "Naked Neck", "Fibromelanosis".
- **Reference:**
    - Lines 216-217: Breed name string matching.
    - Lines 230-247: Trait string matching.

**Action:**
- **Remove** lines 213-260 (Sex-linked, Olive Egger, Na, Fm logic).
- **Replace** with calls to `GeneticsFacade.predict(male, female)`.
- **Logic Change:**
    - Instead of `if (breed == "RIR")`, use `if (genetics.isSexLinkedMatch())`.
    - Instead of string checks, check `phenotypeResult.has(PhenotypeId.NAKED_NECK)`.

## 3. Data Model (`GeneticProfile`)
- Needs extension to support `genotypeCalls` (Gene/Allele pairs) to move away from flat string lists.

## Conclusion
The existing code is incompatible with the product goal of deterministic genetics. The proposed `core/genetics` module must become the single source of truth. Existing files will be refactored to consume this new engine, removing all "magic strings" and "magic numbers" related to inheritance.
