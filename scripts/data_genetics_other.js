module.exports = {
    // --- Ducks ---
    "merchtemse_eend": {
        "baselineGenotype": { "DUCK__C_Locus": "c/c", "DUCK__M_Locus": "M/M" },
        "knownGenes": ["c", "M"], "fixedTraits": ["white_plumage", "blue_bill", "white_eggs"], "confidenceLevel": "medium"
    },
    "dendermonde_duck": {
        "baselineGenotype": { "DUCK__Bl_Locus": "Bl/bl+", "DUCK__M_Locus": "M/M" },
        "knownGenes": ["Bl", "M"], "fixedTraits": ["blue_plumage", "white_bib", "greenish_eggs"], "confidenceLevel": "medium"
    },
    "mandarin": {
        "baselineGenotype": { "SPECIES_Specific": "Aix_galericulata" },
        "knownGenes": [], "fixedTraits": ["ornamental_plumage", "perching_behavior", "cream_eggs"], "confidenceLevel": "low"
    },
    "wood_duck": {
        "baselineGenotype": { "SPECIES_Specific": "Aix_sponsa" },
        "knownGenes": [], "fixedTraits": ["ornamental_plumage", "perching_behavior", "cream_eggs"], "confidenceLevel": "low"
    },
    "rouen_clair": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M", "DUCK__Li_Locus": "Li/Li" },
        "knownGenes": ["M", "Li"], "fixedTraits": ["mallard_pattern", "light_phase", "large_size"], "confidenceLevel": "medium"
    },
    "silver_appleyard": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M", "DUCK__R_Locus": "R/R", "DUCK__Li_Locus": "Li/Li" },
        "knownGenes": ["M", "R", "Li"], "fixedTraits": ["restricted_mallard", "light_phase", "silver_pattern"], "confidenceLevel": "high"
    },
    "orpington_duck": {
        "baselineGenotype": { "DUCK__Bu_Locus": "Bu/Bu" }, // Buff?
        "knownGenes": ["Bu"], "fixedTraits": ["buff_plumage", "white_eggs", "medium_size"], "confidenceLevel": "medium"
    },
    "saxony": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M", "DUCK__Bu_Locus": "Bu/Bu", "DUCK__Li_Locus": "Li/Li" },
        "knownGenes": ["M", "Bu", "Li"], "fixedTraits": ["mallard_pattern", "buff_dilution", "blue_head_drake"], "confidenceLevel": "medium"
    },
    "german_pekin": {
        "baselineGenotype": { "DUCK__C_Locus": "c/c" },
        "knownGenes": ["c"], "fixedTraits": ["white_plumage", "upright_posture", "orange_bill"], "confidenceLevel": "medium"
    },
    "pomeranian_duck": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M", "DUCK__S_Locus": "S/S" }, // Bibbed?
        "knownGenes": ["S"], "fixedTraits": ["black_blue_plumage", "white_bib", "medium_size"], "confidenceLevel": "medium"
    },
    "swedish_yellow_duck": {
        "baselineGenotype": { "DUCK__Y_Locus": "Y/Y" },
        "knownGenes": ["Y"], "fixedTraits": ["yellow_buff_plumage", "blue_green_eggs", "hardy"], "confidenceLevel": "medium"
    },
    "danish_landrace_duck": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M" },
        "knownGenes": ["M"], "fixedTraits": ["wild_type_pattern", "white_bib_variable", "hardy"], "confidenceLevel": "medium"
    },
    "dutch_hookbill": {
        "baselineGenotype": { "DUCK__Hb_Locus": "Hb/Hb" },
        "knownGenes": ["Hb"], "fixedTraits": ["curved_bill", "white_blue_eggs", "variable_color"], "confidenceLevel": "high"
    },
    "dutch_capuchine": {
        "baselineGenotype": { "DUCK__Cr_Locus": "Cr/Cr" },
        "knownGenes": ["Cr"], "fixedTraits": ["crest", "small_size", "ornamental"], "confidenceLevel": "high"
    },
    "drentse_eend": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M" },
        "knownGenes": ["M"], "fixedTraits": ["wild_type", "small_size", "good_forager"], "confidenceLevel": "low"
    },
    "huttegem_duck": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M", "DUCK__Bl_Locus": "Bl/bl+" },
        "knownGenes": ["Bl"], "fixedTraits": ["blue_fawn", "medium_size", "white_eggs"], "confidenceLevel": "medium"
    },
    "duclair": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M", "DUCK__S_Locus": "S/S" },
        "knownGenes": ["S"], "fixedTraits": ["black_plumage", "white_bib", "green_sheen"], "confidenceLevel": "medium"
    },
    "challans": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M", "DUCK__R_Locus": "R/R" }, // Trout/restricted?
        "knownGenes": ["R"], "fixedTraits": ["trout_pattern", "large_size", "flesh_colored_bill"], "confidenceLevel": "medium"
    },
    "italian_white": {
        "baselineGenotype": { "DUCK__C_Locus": "c/c" },
        "knownGenes": ["c"], "fixedTraits": ["white_plumage", "yellow_bill", "small_size"], "confidenceLevel": "medium"
    },
    "ancona_duck": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M", "DUCK__mo_Locus": "mo/mo" }, // Mottled?
        "knownGenes": ["mo"], "fixedTraits": ["broken_pattern", "variable_color", "hardy"], "confidenceLevel": "medium"
    },
    "silver_bantam_duck": {
        "baselineGenotype": { "DUCK__Dw_Locus": "Dw/Dw", "DUCK__R_Locus": "R/R" },
        "knownGenes": ["Dw", "R"], "fixedTraits": ["miniature", "silver_pattern", "fair_layer"], "confidenceLevel": "medium"
    },
    "blue_swedish": {
        "baselineGenotype": { "DUCK__Bl_Locus": "Bl/bl+", "DUCK__S_Locus": "S/S" },
        "knownGenes": ["Bl", "S"], "fixedTraits": ["blue_plumage", "white_bib", "large_size"], "confidenceLevel": "high"
    },
    "buff_orpington_duck": {
        "baselineGenotype": { "DUCK__Bu_Locus": "Bu/Bu" },
        "knownGenes": ["Bu"], "fixedTraits": ["buff_plumage", "white_eggs", "triple_purpose"], "confidenceLevel": "medium"
    },
    "magpie_duck": {
        "baselineGenotype": { "DUCK__M_Locus": "M/M", "DUCK__S_Locus": "S/S" }, // Extreme pied
        "knownGenes": ["S"], "fixedTraits": ["black_white_pattern", "high_egg_production", "light_weight"], "confidenceLevel": "medium"
    },

    // --- Geese ---
    "african_goose": {
        "baselineGenotype": { "GOOSE__Kn_Locus": "Kn/Kn", "GOOSE__De_Locus": "De/De" },
        "knownGenes": ["Kn", "De"], "fixedTraits": ["knob", "dewlap", "brown_stripe"], "confidenceLevel": "high"
    },
    "chinese_goose": {
        "baselineGenotype": { "GOOSE__Kn_Locus": "Kn/Kn" },
        "knownGenes": ["Kn"], "fixedTraits": ["knob", "slender_neck", "high_egg_production"], "confidenceLevel": "high"
    },
    "embden": {
        "baselineGenotype": { "GOOSE__C_Locus": "c/c" }, // White?
        "knownGenes": [], "fixedTraits": ["white_plumage", "blue_eyes", "orange_bill"], "confidenceLevel": "medium"
    },
    "pilgrim": {
        "baselineGenotype": { "GOOSE__Sd_Locus": "Sd/sd+" }, // Auto-sexing
        "knownGenes": ["Sd"], "fixedTraits": ["sex_linked_dimorphism", "white_gander_grey_goose", "medium_size"], "confidenceLevel": "high"
    },
    "sebastopol": {
        "baselineGenotype": { "GOOSE__L_Locus": "L/L" }, // Frizzled/Long feathers
        "knownGenes": ["L"], "fixedTraits": ["curled_feathers", "white_plumage", "unable_to_fly"], "confidenceLevel": "high"
    },
    "toulouse": {
        "baselineGenotype": { "GOOSE__Gy_Locus": "Gy/Gy" }, // Grey
        "knownGenes": [], "fixedTraits": ["grey_plumage", "dewlap", "heavy_weight"], "confidenceLevel": "medium"
    },
    "american_buff_goose": {
        "baselineGenotype": { "GOOSE__Bu_Locus": "Bu/Bu" },
        "knownGenes": ["Bu"], "fixedTraits": ["buff_plumage", "orange_bill", "calm_temperament"], "confidenceLevel": "medium"
    },
    "pomeranian_goose": {
        "baselineGenotype": { "GOOSE__Gy_Locus": "Gy/Gy" }, // Or Saddleback
        "knownGenes": [], "fixedTraits": ["grey_or_buff_saddleback", "pink_bill", "single_lobed_paunch"], "confidenceLevel": "medium"
    },
    "steinbacher": {
        "baselineGenotype": { "GOOSE__Bl_Locus": "Bl/bl+" }, // Blue?
        "knownGenes": ["Bl"], "fixedTraits": ["blue_grey_plumage", "black_serrated_bill", "fighting_origin"], "confidenceLevel": "medium"
    },
    "canada_goose": {
        "baselineGenotype": { "SPECIES_Specific": "Branta_canadensis" },
        "knownGenes": [], "fixedTraits": ["black_head_white_cheek", "wild_type", "monogamous"], "confidenceLevel": "high"
    },
    "roman_tufted": {
        "baselineGenotype": { "GOOSE__Cr_Locus": "Cr/Cr" },
        "knownGenes": ["Cr"], "fixedTraits": ["crest", "white_plumage", "small_size"], "confidenceLevel": "high"
    },

    // --- Turkeys ---
    "bronze_turkey": {
        "baselineGenotype": { "TURKEY__b_Locus": "b+/b+" },
        "knownGenes": ["b+"], "fixedTraits": ["bronze_plumage", "iridescent", "large_size"], "confidenceLevel": "high"
    },
    "ronquieres_turkey": {
        "baselineGenotype": { "TURKEY__e_Locus": "e/e" }, // Cröllwitzer-like pattern?
        "knownGenes": [], "fixedTraits": ["hermigon_pattern", "small_size", "hardy"], "confidenceLevel": "medium"
    },
    "norfolk_black": {
        "baselineGenotype": { "TURKEY__B_Locus": "B/B" },
        "knownGenes": ["B"], "fixedTraits": ["black_plumage", "medium_size", "excellent_meat"], "confidenceLevel": "medium"
    },
    "cambridge_bronze": {
        "baselineGenotype": { "TURKEY__b_Locus": "b+/b+" },
        "knownGenes": ["b+"], "fixedTraits": ["bronze_plumage", "traditional_breed", "medium_size"], "confidenceLevel": "medium"
    },
    "crollwitzer_turkey": {
        "baselineGenotype": { "TURKEY__p_Locus": "p/p" }, // Palm?
        "knownGenes": ["p"], "fixedTraits": ["pied_pattern", "ornamental", "small_size"], "confidenceLevel": "medium"
    },
    "diepholzer_turkey": {
        "baselineGenotype": { "TURKEY__c_Locus": "c/c" },
        "knownGenes": ["c"], "fixedTraits": ["white_plumage", "medium_size", "hardy"], "confidenceLevel": "medium"
    },
    "german_black_turkey": {
        "baselineGenotype": { "TURKEY__B_Locus": "B/B" },
        "knownGenes": ["B"], "fixedTraits": ["black_plumage", "red_caruncles", "clean_legs"], "confidenceLevel": "medium"
    },
    "italian_bronze_turkey": {
        "baselineGenotype": { "TURKEY__b_Locus": "b+/b+" },
        "knownGenes": ["b+"], "fixedTraits": ["bronze_plumage", "smaller_than_standard_bronze"], "confidenceLevel": "medium"
    },
    "ermellinato_di_rovigo": {
        "baselineGenotype": { "TURKEY__n_Locus": "n/n", "TURKEY__p_Locus": "p/p" }, // Narragansett+Palm?
        "knownGenes": [], "fixedTraits": ["ermine_pattern", "cotton_turkey", "fast_growing"], "confidenceLevel": "low"
    },
    "spanish_black_turkey": {
        "baselineGenotype": { "TURKEY__B_Locus": "B/B" },
        "knownGenes": ["B"], "fixedTraits": ["black_plumage", "white_skin", "traditional"], "confidenceLevel": "medium"
    },
    "dutch_white_turkey": {
        "baselineGenotype": { "TURKEY__c_Locus": "c/c" },
        "knownGenes": ["c"], "fixedTraits": ["white_plumage", "small_size", "ornamental"], "confidenceLevel": "medium"
    },
    "sologne_turkey": {
        "baselineGenotype": { "TURKEY__B_Locus": "B/B" },
        "knownGenes": ["B"], "fixedTraits": ["black_plumage", "blue_head_skin", "hardy"], "confidenceLevel": "medium"
    },
    "broad_breasted_white": {
        "baselineGenotype": { "TURKEY__c_Locus": "c/c" },
        "knownGenes": ["c"], "fixedTraits": ["white_plumage", "rapid_growth", "huge_breast"], "confidenceLevel": "high"
    },
    "slate_turkey": {
        "baselineGenotype": { "TURKEY__sl_Locus": "sl/sl" },
        "knownGenes": ["sl"], "fixedTraits": ["slate_blue_plumage", "variable_shade", "medium_size"], "confidenceLevel": "high"
    },

    // --- Peafowl ---
    "indian_blue_peafowl": {
        "baselineGenotype": { "PEAFOWL__IB_Locus": "IB/IB" },
        "knownGenes": ["IB"], "fixedTraits": ["blue_neck", "barred_wings", "train_feathers"], "confidenceLevel": "high"
    },
    "white_peafowl": {
        "baselineGenotype": { "PEAFOWL__w_Locus": "w/w" },
        "knownGenes": ["w"], "fixedTraits": ["white_plumage", "blue_eyes", "same_structure_as_blue"], "confidenceLevel": "high"
    },
    "cameo_peafowl": {
        "baselineGenotype": { "PEAFOWL__ca_Locus": "ca/ca" }, // Sex-linked?
        "knownGenes": ["ca"], "fixedTraits": ["brown_neck", "cream_pattern", "lighter_train"], "confidenceLevel": "medium"
    },
    "black_shouldered_peafowl": {
        "baselineGenotype": { "PEAFOWL__bs_Locus": "bs/bs" },
        "knownGenes": ["bs"], "fixedTraits": ["black_shoulders", "solid_wing_color", "white_hen"], "confidenceLevel": "high"
    },
    "purple_peafowl": {
        "baselineGenotype": { "PEAFOWL__pu_Locus": "pu/pu" }, // Sex-linked?
        "knownGenes": ["pu"], "fixedTraits": ["purple_neck_sheen", "darker_blue", "purple_train_eyes"], "confidenceLevel": "medium"
    },
    "spalding_peafowl": {
        "baselineGenotype": { "PEAFOWL__S_Locus": "S/S" }, // Hybrid
        "knownGenes": [], "fixedTraits": ["green_neck_sheen", "taller", "hybrid_vigor"], "confidenceLevel": "medium"
    },
    "green_peafowl": {
        "baselineGenotype": { "SPECIES_Specific": "Pavo_muticus" },
        "knownGenes": [], "fixedTraits": ["green_neck", "scaly_feathers", "upright_crest"], "confidenceLevel": "high"
    },

    // --- Pheasants ---
    "ring_necked_pheasant": {
        "baselineGenotype": { "PHEASANT__Wild": "wt/wt" },
        "knownGenes": [], "fixedTraits": ["white_neck_ring", "long_tail", "game_bird"], "confidenceLevel": "high"
    },
    "mongolian_pheasant": {
        "baselineGenotype": { "PHEASANT__M_Locus": "M/M" },
        "knownGenes": [], "fixedTraits": ["darker_plumage", "broad_white_ring", "large_size"], "confidenceLevel": "medium"
    },
    "reeves_pheasant": {
        "baselineGenotype": { "SPECIES_Specific": "Syrmaticus_reevesii" },
        "knownGenes": [], "fixedTraits": ["golden_plumage", "extremely_long_tail", "aggressive"], "confidenceLevel": "high"
    },
    "golden_pheasant": {
        "baselineGenotype": { "SPECIES_Specific": "Chrysolophus_pictus" },
        "knownGenes": [], "fixedTraits": ["golden_crest", "red_breast", "ruff"], "confidenceLevel": "high"
    },
    "lady_amherst_pheasant": {
        "baselineGenotype": { "SPECIES_Specific": "Chrysolophus_amherstiae" },
        "knownGenes": [], "fixedTraits": ["white_black_crest", "green_breast", "long_tail"], "confidenceLevel": "high"
    },
    "silver_pheasant": {
        "baselineGenotype": { "SPECIES_Specific": "Lophura_nycthemera" },
        "knownGenes": [], "fixedTraits": ["silver_white_upper", "black_under", "red_face_wattles"], "confidenceLevel": "high"
    },

    // --- Quail ---
    "coturnix_japanese": {
        "baselineGenotype": { "QUAIL__Wild": "wt/wt" },
        "knownGenes": [], "fixedTraits": ["brown_speckled", "fast_maturing", "high_production"], "confidenceLevel": "high"
    },
    "jumbo_coturnix": {
        "baselineGenotype": { "QUAIL__Wild": "wt/wt" }, // Selection for size
        "knownGenes": [], "fixedTraits": ["large_size", "brown_speckled", "meat_bird"], "confidenceLevel": "high"
    },
    "pharaoh_coturnix": {
        "baselineGenotype": { "QUAIL__Wild": "wt/wt" },
        "knownGenes": [], "fixedTraits": ["wild_type_plumage", "standard_size", "good_layer"], "confidenceLevel": "medium"
    },
    "tibetan_coturnix": {
        "baselineGenotype": { "QUAIL__E_Locus": "E/E" }, // Extended brown/black?
        "knownGenes": [], "fixedTraits": ["dark_brown_plumage", "white_breast_patch_variable"], "confidenceLevel": "medium"
    },
    "bobwhite_quail": {
        "baselineGenotype": { "SPECIES_Specific": "Colinus_virginianus" },
        "knownGenes": [], "fixedTraits": ["white_throat_male", "brown_body", "game_bird"], "confidenceLevel": "high"
    },
    "california_quail": {
        "baselineGenotype": { "SPECIES_Specific": "Callipepla_californica" },
        "knownGenes": [], "fixedTraits": ["topknot_crest", "grey_plumage", "scaled_belly"], "confidenceLevel": "high"
    },
    "button_quail": {
        "baselineGenotype": { "SPECIES_Specific": "Coturnix_chinensis" },
        "knownGenes": [], "fixedTraits": ["miniature_size", "painted_face_male", "ground_dweller"], "confidenceLevel": "medium"
    },
    "blue_scale_quail": {
        "baselineGenotype": { "SPECIES_Specific": "Callipepla_squamata" },
        "knownGenes": [], "fixedTraits": ["scaled_pattern", "blue_grey_plumage", "cotton_top_crest"], "confidenceLevel": "medium"
    },
    "mountain_quail": {
        "baselineGenotype": { "SPECIES_Specific": "Oreortyx_pictus" },
        "knownGenes": [], "fixedTraits": ["straight_plume_crest", "large_size", "chestnut_throat"], "confidenceLevel": "medium"
    }
};
