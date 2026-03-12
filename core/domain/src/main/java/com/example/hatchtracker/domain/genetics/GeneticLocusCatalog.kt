package com.example.hatchtracker.domain.genetics

import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.genetics.DominanceType
import com.example.hatchtracker.model.genetics.InheritanceType
import com.example.hatchtracker.model.genetics.LocusDefinition

/**
 * SINGLE SOURCE OF TRUTH for loci + allele sets + basic phenotype rules mapping hooks.
 * Expand over time; keep locus IDs stable.
 */
object GeneticLocusCatalog {

    // Locus IDs
    const val LOCUS_O = "O_Locus"          // Blue egg structural
    const val LOCUS_BR = "BR_Locus"        // Brown overlay (simplified)
    const val LOCUS_B = "B_Locus"          // Sex-linked barring (Z)
    const val LOCUS_BL = "Bl_Locus"        // Blue dilution
    const val LOCUS_NA = "Na_Locus"        // Naked neck
    const val LOCUS_FM = "Fm_Locus"        // Fibromelanosis
    const val LOCUS_E = "E_Locus"          // Extended black (simplified)

    /**
     * Allele conventions:
     * - Use explicit wildtype markers like "o" or "br+" consistently.
     * - Keep the strings compatible with your seed baselineGenotype "O/o" etc.
     */
    val loci: Map<String, LocusDefinition> = mapOf(
        LOCUS_O to LocusDefinition(
            locusId = LOCUS_O,
            displayName = "Blue Egg (O)",
            inheritance = InheritanceType.AUTOSOMAL,
            dominance = DominanceType.DOMINANT,
            alleles = setOf("O", "o"),
            defaultWildtype = "o",
            notes = "O is dominant blue shell structural gene."
        ),
        LOCUS_BR to LocusDefinition(
            locusId = LOCUS_BR,
            displayName = "Brown Overlay (BR)",
            inheritance = InheritanceType.AUTOSOMAL,
            dominance = DominanceType.DOMINANT,
            alleles = setOf("BR", "br+"),
            defaultWildtype = "br+",
            notes = "Simplified brown overlay presence. Real brown is polygenic; treat as dominant overlay flag."
        ),
        LOCUS_B to LocusDefinition(
            locusId = LOCUS_B,
            displayName = "Barring (B) (Sex-linked)",
            inheritance = InheritanceType.Z_LINKED,
            dominance = DominanceType.DOMINANT,
            alleles = setOf("B", "b+"),
            defaultWildtype = "b+",
            notes = "Z-linked barring. Roosters ZZ, hens ZW."
        ),
        LOCUS_BL to LocusDefinition(
            locusId = LOCUS_BL,
            displayName = "Blue Dilution (Bl)",
            inheritance = InheritanceType.AUTOSOMAL,
            dominance = DominanceType.INCOMPLETE,
            alleles = setOf("Bl", "bl+"),
            defaultWildtype = "bl+",
            notes = "Incomplete dominance: Bl/bl+ = Blue; Bl/Bl = Splash; bl+/bl+ = Black base (no dilution)."
        ),
        LOCUS_NA to LocusDefinition(
            locusId = LOCUS_NA,
            displayName = "Naked Neck (Na)",
            inheritance = InheritanceType.AUTOSOMAL,
            dominance = DominanceType.DOMINANT,
            alleles = setOf("Na", "na+"),
            defaultWildtype = "na+",
            notes = "Dominant Na causes naked neck trait."
        ),
        LOCUS_FM to LocusDefinition(
            locusId = LOCUS_FM,
            displayName = "Fibromelanosis (Fm)",
            inheritance = InheritanceType.AUTOSOMAL,
            dominance = DominanceType.DOMINANT,
            alleles = setOf("Fm", "fm+"),
            defaultWildtype = "fm+",
            notes = "Dominant Fm produces black skin (Silkie-type)."
        ),
        LOCUS_E to LocusDefinition(
            locusId = LOCUS_E,
            displayName = "Extended Black (E)",
            inheritance = InheritanceType.AUTOSOMAL,
            dominance = DominanceType.DOMINANT,
            alleles = setOf("E", "e+", "eb"),
            defaultWildtype = "e+",
            notes = "Simplified E-locus: E is dominant extended black; e+ wildtype; eb brown. Expand later."
        ),
        "S_Locus" to LocusDefinition(
            locusId = "S_Locus",
            displayName = "Silver (S)",
            inheritance = InheritanceType.Z_LINKED,
            dominance = DominanceType.DOMINANT,
            alleles = setOf("S", "s+"),
            defaultWildtype = "s+"
        ),
        "I_Locus" to LocusDefinition(
            locusId = "I_Locus",
            displayName = "Dominant White (I)",
            inheritance = InheritanceType.AUTOSOMAL,
            dominance = DominanceType.DOMINANT,
            alleles = setOf("I", "i+"),
            defaultWildtype = "i+"
        ),
        "Hf_Locus" to LocusDefinition(
            locusId = "Hf_Locus",
            displayName = "Hen Feathering (Hf)",
            inheritance = InheritanceType.AUTOSOMAL,
            dominance = DominanceType.DOMINANT,
            alleles = setOf("Hf", "hf+"),
            defaultWildtype = "hf+"
        ),

        // --- EXTENDED CHICKEN LOCI ---
        "Ab_Locus" to LocusDefinition(locusId="Ab_Locus", displayName="Autosomal Barring (Ab)", alleles=setOf("Ab", "ab+"), defaultWildtype="ab+"),
        "Bc_Locus" to LocusDefinition(locusId="Bc_Locus", displayName="Buttercup Comb (Bc)", alleles=setOf("Bc", "bc+"), defaultWildtype="bc+"),
        "Co_Locus" to LocusDefinition(locusId="Co_Locus", displayName="Columbian (Co)", alleles=setOf("Co", "co+"), defaultWildtype="co+"),
        "Cr_Locus" to LocusDefinition(locusId="Cr_Locus", displayName="Crest (Cr)", alleles=setOf("Cr", "cr+"), defaultWildtype="cr+"),
        "Dv_Locus" to LocusDefinition(locusId="Dv_Locus", displayName="Duplex Comb (Dv)", alleles=setOf("Dv", "dv+"), defaultWildtype="dv+"),
        "Gt_Locus" to LocusDefinition(locusId="Gt_Locus", displayName="Gradient (Gt)", alleles=setOf("Gt", "gt+"), defaultWildtype="gt+"),
        "H_Locus" to LocusDefinition(locusId="H_Locus", displayName="Silkie Feathering (h)", alleles=setOf("H", "h"), defaultWildtype="H", dominance=DominanceType.RECESSIVE, notes="h is recessive (Silkie), H is wildtype (Normal)"),
        "Id_Locus" to LocusDefinition(locusId="Id_Locus", displayName="Dermal Melanin Inhibitor (Id)", alleles=setOf("Id", "id+"), defaultWildtype="id+", inheritance=InheritanceType.Z_LINKED),
        "id_Locus" to LocusDefinition(locusId="id_Locus", displayName="Dermal Melanin Inhibitor (legacy id)", alleles=setOf("Id", "id+"), defaultWildtype="id+", inheritance=InheritanceType.Z_LINKED, notes="Alias for Id_Locus"),
        "Lg_Locus" to LocusDefinition(locusId="Lg_Locus", displayName="Lacing (Lg)", alleles=setOf("Lg", "lg+"), defaultWildtype="lg+"),
        "Mb_Locus" to LocusDefinition(locusId="Mb_Locus", displayName="Muffs/Beard (Mb)", alleles=setOf("Mb", "mb+"), defaultWildtype="mb+"),
        "Mh_Locus" to LocusDefinition(locusId="Mh_Locus", displayName="Mahogany (Mh)", alleles=setOf("Mh", "mh+"), defaultWildtype="mh+"),
        "Mo_Locus" to LocusDefinition(locusId="Mo_Locus", displayName="Mottling (Mo)", alleles=setOf("Mo", "mo"), defaultWildtype="Mo", dominance=DominanceType.RECESSIVE, notes="mo is recessive"),
        "mo_Locus" to LocusDefinition(locusId="mo_Locus", displayName="Mottling (legacy mo)", alleles=setOf("Mo", "mo"), defaultWildtype="Mo", dominance=DominanceType.RECESSIVE, notes="Alias for Mo_Locus"),
        "nm_Locus" to LocusDefinition(locusId="nm_Locus", displayName="Non-Molting (nm)", alleles=setOf("Nm", "nm"), defaultWildtype="Nm", dominance=DominanceType.RECESSIVE),
        "P_Locus" to LocusDefinition(locusId="P_Locus", displayName="Pea Comb (P)", alleles=setOf("P", "p+"), defaultWildtype="p+"),
        "Po_Locus" to LocusDefinition(locusId="Po_Locus", displayName="Polydactyly (Po)", alleles=setOf("Po", "po+"), defaultWildtype="po+"),
        "Pr_Locus" to LocusDefinition(locusId="Pr_Locus", displayName="Protoporphyrin (Pr)", alleles=setOf("Pr", "pr+"), defaultWildtype="pr+"),
        "Pti_Locus" to LocusDefinition(locusId="Pti_Locus", displayName="Ptilopody (Pti)", alleles=setOf("Pti-1", "pti+"), defaultWildtype="pti+"),
        "R_Locus" to LocusDefinition(locusId="R_Locus", displayName="Rose Comb (R)", alleles=setOf("R", "r+"), defaultWildtype="r+"),
        "Rp_Locus" to LocusDefinition(locusId="Rp_Locus", displayName="Rumpless (Rp)", alleles=setOf("Rp", "rp+"), defaultWildtype="rp+"),
        "Sp_Locus" to LocusDefinition(locusId="Sp_Locus", displayName="Spangling (Sp)", alleles=setOf("Sp", "sp+"), defaultWildtype="sp+"),
        "v_Locus" to LocusDefinition(locusId="v_Locus", displayName="V-Comb (v)", alleles=setOf("V", "v"), defaultWildtype="V", dominance=DominanceType.RECESSIVE),
        "W_Locus" to LocusDefinition(locusId="W_Locus", displayName="Yellow Skin (w)", alleles=setOf("W", "w+"), defaultWildtype="W"),
        "c_Locus" to LocusDefinition(locusId="c_Locus", displayName="Recessive White (c)", alleles=setOf("C", "c"), defaultWildtype="C", dominance=DominanceType.RECESSIVE),

        // --- NON-CHICKEN LOCI (EXTENDED) ---
        "DUCK__Bl_Locus" to LocusDefinition(locusId="DUCK__Bl_Locus", displayName="Blue Dilution (Bl)", alleles=setOf("Bl", "bl+"), defaultWildtype="bl+"),
        "DUCK__Bu_Locus" to LocusDefinition(locusId="DUCK__Bu_Locus", displayName="Buff (Bu)", alleles=setOf("Bu", "bu+"), defaultWildtype="bu+"),
        "DUCK__C_Locus" to LocusDefinition(locusId="DUCK__C_Locus", displayName="Color (C) / White", alleles=setOf("C", "c+"), defaultWildtype="C"),
        "DUCK__Cr_Locus" to LocusDefinition(locusId="DUCK__Cr_Locus", displayName="Crest (Cr)", alleles=setOf("Cr", "cr+"), defaultWildtype="cr+"),
        "DUCK__D_Locus" to LocusDefinition(locusId="DUCK__D_Locus", displayName="Dilution (D)", alleles=setOf("D", "d+"), defaultWildtype="d+"),
        "DUCK__Dw_Locus" to LocusDefinition(locusId="DUCK__Dw_Locus", displayName="Dwarf (Dw)", alleles=setOf("Dw", "dw+"), defaultWildtype="dw+"),
        "DUCK__E_Locus" to LocusDefinition(locusId="DUCK__E_Locus", displayName="Extended Black (E)", alleles=setOf("E", "e+"), defaultWildtype="e+"),
        "DUCK__Hb_Locus" to LocusDefinition(locusId="DUCK__Hb_Locus", displayName="Hookbill (Hb)", alleles=setOf("Hb", "hb+"), defaultWildtype="hb+"),
        "DUCK__Li_Locus" to LocusDefinition(locusId="DUCK__Li_Locus", displayName="Light Phase (Li)", alleles=setOf("Li", "li+"), defaultWildtype="li+"),
        "DUCK__M_Locus" to LocusDefinition(locusId="DUCK__M_Locus", displayName="Mallard (M)", alleles=setOf("M", "m+"), defaultWildtype="M", notes="M is Mallard pattern"),
        "DUCK__mo_Locus" to LocusDefinition(locusId="DUCK__mo_Locus", displayName="Mottling (mo)", alleles=setOf("Mo", "mo"), defaultWildtype="Mo", dominance=DominanceType.RECESSIVE),
        "DUCK__P_Locus" to LocusDefinition(locusId="DUCK__P_Locus", displayName="Pied (P)", alleles=setOf("P", "p+"), defaultWildtype="p+"),
        "DUCK__R_Locus" to LocusDefinition(locusId="DUCK__R_Locus", displayName="Runner/Restricted (R)", alleles=setOf("R", "r+"), defaultWildtype="r+"),
        "DUCK__S_Locus" to LocusDefinition(locusId="DUCK__S_Locus", displayName="Bibbed (S)", alleles=setOf("S", "s+"), defaultWildtype="s+"),
        "DUCK__Y_Locus" to LocusDefinition(locusId="DUCK__Y_Locus", displayName="Yellow Bill (Y)", alleles=setOf("Y", "y+"), defaultWildtype="Y"),
        // Legacy DUCK:
        "DUCK__COL_Locus" to LocusDefinition(locusId="DUCK__COL_Locus", displayName="Color (Col)", alleles=setOf("Col", "col+"), defaultWildtype="col+"),
        "DUCK__DIL_Locus" to LocusDefinition(locusId="DUCK__DIL_Locus", displayName="Dilution (Dil)", alleles=setOf("Dil", "dil+"), defaultWildtype="dil+"),
        "DUCK__PAT_Locus" to LocusDefinition(locusId="DUCK__PAT_Locus", displayName="Pattern (Pat)", alleles=setOf("Pat", "pat+"), defaultWildtype="pat+"),
        "DUCK__MEL_Locus" to LocusDefinition(locusId="DUCK__MEL_Locus", displayName="Melanin (Mel)", alleles=setOf("Mel", "mel+"), defaultWildtype="mel+"),
        "DUCK__ES_Locus" to LocusDefinition(
            locusId = "DUCK__ES_Locus", displayName = "Egg Shell (ES)", inheritance = InheritanceType.AUTOSOMAL, dominance = DominanceType.DOMINANT, alleles = setOf("ES", "es+"), defaultWildtype = "es+", notes = "Simplified presence flag for egg shell tint/overlay."
        ),

        "GOOSE__Bl_Locus" to LocusDefinition(locusId="GOOSE__Bl_Locus", displayName="Blue (Bl)", alleles=setOf("Bl", "bl+"), defaultWildtype="bl+"),
        "GOOSE__Bu_Locus" to LocusDefinition(locusId="GOOSE__Bu_Locus", displayName="Buff (Bu)", alleles=setOf("Bu", "bu+"), defaultWildtype="bu+"),
        "GOOSE__C_Locus" to LocusDefinition(locusId="GOOSE__C_Locus", displayName="Color (C)", alleles=setOf("C", "c"), defaultWildtype="C"),
        "GOOSE__Cr_Locus" to LocusDefinition(locusId="GOOSE__Cr_Locus", displayName="Crest (Cr)", alleles=setOf("Cr", "cr+"), defaultWildtype="cr+"),
        "GOOSE__De_Locus" to LocusDefinition(locusId="GOOSE__De_Locus", displayName="Dewlap (De)", alleles=setOf("De", "de+"), defaultWildtype="de+"),
        "GOOSE__Gy_Locus" to LocusDefinition(locusId="GOOSE__Gy_Locus", displayName="Grey (Gy)", alleles=setOf("Gy", "gy+"), defaultWildtype="gy+"),
        "GOOSE__Kn_Locus" to LocusDefinition(locusId="GOOSE__Kn_Locus", displayName="Knob (Kn)", alleles=setOf("Kn", "kn+"), defaultWildtype="kn+"),
        "GOOSE__L_Locus" to LocusDefinition(locusId="GOOSE__L_Locus", displayName="Long Feather (L)", alleles=setOf("L", "l+"), defaultWildtype="l+"),
        "GOOSE__Sd_Locus" to LocusDefinition(locusId="GOOSE__Sd_Locus", displayName="Saddleback (Sd)", alleles=setOf("Sd", "sd+"), defaultWildtype="sd+"),
        // Legacy GOOSE:
        "GOOSE__ES_Locus" to LocusDefinition(locusId="GOOSE__ES_Locus", displayName="Egg Shell (ES)", alleles=setOf("ES", "es+"), defaultWildtype="es+"),
        "GOOSE__DIL_Locus" to LocusDefinition(locusId="GOOSE__DIL_Locus", displayName="Dilution (Dil)", alleles=setOf("Dil", "dil+"), defaultWildtype="dil+"),
        "GOOSE__MEL_Locus" to LocusDefinition(locusId="GOOSE__MEL_Locus", displayName="Melanin (Mel)", alleles=setOf("Mel", "mel+"), defaultWildtype="mel+"),

        "TURKEY__B_Locus" to LocusDefinition(locusId="TURKEY__B_Locus", displayName="Black (B)", alleles=setOf("B", "b+"), defaultWildtype="b+"),
        "TURKEY__b_Locus" to LocusDefinition(locusId="TURKEY__b_Locus", displayName="Bronze (b+)", alleles=setOf("b+", "b"), defaultWildtype="b+"),
        "TURKEY__C_Locus" to LocusDefinition(locusId="TURKEY__C_Locus", displayName="Color (C)", alleles=setOf("C", "c"), defaultWildtype="C"),
        "TURKEY__c_Locus" to LocusDefinition(locusId="TURKEY__c_Locus", displayName="White (c)", alleles=setOf("C", "c"), defaultWildtype="C", dominance=DominanceType.RECESSIVE),
        "TURKEY__e_Locus" to LocusDefinition(locusId="TURKEY__e_Locus", displayName="Ermine (e)", alleles=setOf("E", "e"), defaultWildtype="E"),
        "TURKEY__n_Locus" to LocusDefinition(locusId="TURKEY__n_Locus", displayName="Narragansett (n)", alleles=setOf("N", "n"), defaultWildtype="N", dominance=DominanceType.RECESSIVE),
        "TURKEY__N_Locus" to LocusDefinition(locusId="TURKEY__N_Locus", displayName="Narragansett (N)", alleles=setOf("N", "n"), defaultWildtype="N"),
        "TURKEY__p_Locus" to LocusDefinition(locusId="TURKEY__p_Locus", displayName="Palm (p)", alleles=setOf("P", "p"), defaultWildtype="P", dominance=DominanceType.RECESSIVE),
        "TURKEY__P_Locus" to LocusDefinition(locusId="TURKEY__P_Locus", displayName="Palm (P)", alleles=setOf("P", "p"), defaultWildtype="P"),
        "TURKEY__R_Locus" to LocusDefinition(locusId="TURKEY__R_Locus", displayName="Red (r)", alleles=setOf("R", "r"), defaultWildtype="R", dominance=DominanceType.RECESSIVE),
        "TURKEY__sl_Locus" to LocusDefinition(locusId="TURKEY__sl_Locus", displayName="Slate (sl)", alleles=setOf("Sl", "sl"), defaultWildtype="Sl"),
        // Legacy TURKEY:
        "TURKEY__COL_Locus" to LocusDefinition(locusId="TURKEY__COL_Locus", displayName="Color (Col)", alleles=setOf("Col", "col+"), defaultWildtype="col+"),
        "TURKEY__DIL_Locus" to LocusDefinition(locusId="TURKEY__DIL_Locus", displayName="Dilution (Dil)", alleles=setOf("Dil", "dil+"), defaultWildtype="dil+"),
        "TURKEY__PAT_Locus" to LocusDefinition(locusId="TURKEY__PAT_Locus", displayName="Pattern (Pat)", alleles=setOf("Pat", "pat+"), defaultWildtype="pat+"),

        "PEAFOWL__bs_Locus" to LocusDefinition(locusId="PEAFOWL__bs_Locus", displayName="Black Shoulder (bs)", alleles=setOf("Bs", "bs"), defaultWildtype="Bs"),
        "PEAFOWL__ca_Locus" to LocusDefinition(locusId="PEAFOWL__ca_Locus", displayName="Cameo (ca)", alleles=setOf("Ca", "ca"), defaultWildtype="Ca"),
        "PEAFOWL__IB_Locus" to LocusDefinition(locusId="PEAFOWL__IB_Locus", displayName="India Blue (IB)", alleles=setOf("IB", "ib"), defaultWildtype="IB"),
        "PEAFOWL__pu_Locus" to LocusDefinition(locusId="PEAFOWL__pu_Locus", displayName="Purple (pu)", alleles=setOf("Pu", "pu"), defaultWildtype="Pu"),
        "PEAFOWL__S_Locus" to LocusDefinition(locusId="PEAFOWL__S_Locus", displayName="Spalding (S)", alleles=setOf("S", "s+"), defaultWildtype="s+"),
        "PEAFOWL__w_Locus" to LocusDefinition(locusId="PEAFOWL__w_Locus", displayName="White (w)", alleles=setOf("W", "w"), defaultWildtype="W", dominance=DominanceType.RECESSIVE),
        // Legacy PEAFOWL:
        "PEAFOWL__COL_Locus" to LocusDefinition(locusId="PEAFOWL__COL_Locus", displayName="Color (Col)", alleles=setOf("Col", "col+"), defaultWildtype="col+"),
        "PEAFOWL__WHT_Locus" to LocusDefinition(locusId="PEAFOWL__WHT_Locus", displayName="White (Wht)", alleles=setOf("Wht", "wht+"), defaultWildtype="wht+", dominance=DominanceType.RECESSIVE),
        "PEAFOWL__PAT_Locus" to LocusDefinition(locusId="PEAFOWL__PAT_Locus", displayName="Pattern (Pat)", alleles=setOf("Pat", "pat+"), defaultWildtype="pat+"),

        "PHEASANT__M_Locus" to LocusDefinition(locusId="PHEASANT__M_Locus", displayName="Melanistic (M)", alleles=setOf("M", "m+"), defaultWildtype="m+"),
        "PHEASANT__Mel_Locus" to LocusDefinition(locusId="PHEASANT__Mel_Locus", displayName="Melanistic (Mel)", alleles=setOf("Mel", "mel+"), defaultWildtype="mel+"),
        "PHEASANT__Wild" to LocusDefinition(locusId="PHEASANT__Wild", displayName="Wild Type", alleles=setOf("wt"), defaultWildtype="wt"),
        // Legacy PHEASANT:
        "PHEASANT__COL_Locus" to LocusDefinition(locusId="PHEASANT__COL_Locus", displayName="Color (Col)", alleles=setOf("Col", "col+"), defaultWildtype="col+"),
        "PHEASANT__DIL_Locus" to LocusDefinition(locusId="PHEASANT__DIL_Locus", displayName="Dilution (Dil)", alleles=setOf("Dil", "dil+"), defaultWildtype="dil+"),
        "PHEASANT__PAT_Locus" to LocusDefinition(locusId="PHEASANT__PAT_Locus", displayName="Pattern (Pat)", alleles=setOf("Pat", "pat+"), defaultWildtype="pat+"),

        "QUAIL__E_Locus" to LocusDefinition(locusId="QUAIL__E_Locus", displayName="Red/Brown (E)", alleles=setOf("E", "e+"), defaultWildtype="e+"),
        "QUAIL__Wild" to LocusDefinition(locusId="QUAIL__Wild", displayName="Wild Type", alleles=setOf("wt"), defaultWildtype="wt"),
        // Legacy QUAIL:
        "QUAIL__COL_Locus" to LocusDefinition(locusId="QUAIL__COL_Locus", displayName="Color (Col)", alleles=setOf("Col", "col+"), defaultWildtype="col+"),
        "QUAIL__DIL_Locus" to LocusDefinition(locusId="QUAIL__DIL_Locus", displayName="Dilution (Dil)", alleles=setOf("Dil", "dil+"), defaultWildtype="dil+"),
        "QUAIL__PAT_Locus" to LocusDefinition(locusId="QUAIL__PAT_Locus", displayName="Pattern (Pat)", alleles=setOf("Pat", "pat+"), defaultWildtype="pat+"),

        "SPECIES_Specific" to LocusDefinition(locusId="SPECIES_Specific", displayName="Species Specific", alleles=setOf("Any"), defaultWildtype="Any")
    )

    /**
     * Filters loci by species.
     * RULES:
     * - Chicken uses legacy IDs (no double underscore).
     * - All other species MUST use SPECIES__ prefix.
     * - Mixed locus ban: If species != CHICKEN, loci without "__" are ignored.
     */
    fun lociForSpecies(species: Species): List<LocusDefinition> {
        return when (species) {
            Species.CHICKEN -> loci.values.filter { isChickenLegacyLocusId(it.locusId) }
            else -> {
                val prefix = speciesPrefix(species)
                loci.values.filter { it.locusId.startsWith(prefix) }
            }
        }
    }

    /**
     * Helper to get the namespacing prefix for a species.
     */
    fun speciesPrefix(species: Species): String {
        return if (species == Species.CHICKEN) "" else "${species.name.uppercase()}__"
    }

    /**
     * Returns true if the locus ID follows the multi-species namespacing convention.
     */
    fun isNamespacedLocusId(locusId: String): Boolean {
        return locusId.contains("__")
    }

    /**
     * Returns true if the locus ID is a legacy Chicken locus ID (no namespacing).
     */
    fun isChickenLegacyLocusId(locusId: String): Boolean {
        return !isNamespacedLocusId(locusId)
    }

    fun assertAllLociExist(locusIds: List<String>) {
        locusIds.forEach { requireLocus(it) }
    }

    fun requireLocus(locusId: String): LocusDefinition =
        loci[locusId] ?: error("Unknown locusId: $locusId")

    /**
     * Helper to validate allele strings during parsing or seeding.
     */
    fun isValidAllele(locusId: String, allele: String): Boolean {
        val def = loci[locusId] ?: return false
        return allele in def.alleles
    }
}

