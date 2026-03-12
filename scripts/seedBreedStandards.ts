import * as admin from 'firebase-admin';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Interface for Breed Standard data
 */
interface GeneticProfile {
    knownGenes: string[];            // confirmed gene symbols only
    fixedTraits: string[];           // phenotype traits guaranteed by the breed
    inferredTraits?: string[];        // clearly labeled inferred traits
    unknownTraits?: string[];         // explicitly unknown areas
    confidenceLevel: "high" | "medium" | "low";

    /**
     * NEW — Structured genotype blueprint for deterministic engine
     * Key = Locus ID
     * Value = Allele pair string (e.g., "O/o")
     */
    baselineGenotype?: Record<string, string>;
}

interface BreedStandard {
    id: string; // Used as the Firestore Document ID (breedId, stable, lowercase)
    name: string;
    origin: string;
    species: "Chicken" | "Duck" | "Goose" | "Turkey" | "Peafowl" | "Pheasant" | "Quail";
    eggColor: string;
    acceptedColors: string[];
    weightRoosterKg: number; // For non-chickens, interpretable as Male weight
    weightHenKg: number;     // For non-chickens, interpretable as Female weight
    size?: "Small" | "Medium" | "Large";
    weightClass?: "Light" | "Medium" | "Heavy";
    eggProduction?: number;   // Average eggs per year
    official: boolean;
    lastUpdated?: admin.firestore.Timestamp | admin.firestore.FieldValue;
    recognizedBy: string[];
    geneticProfile: GeneticProfile;
    category?: "large_fowl" | "bantam" | "ornamental";
    isTrueBantam?: boolean;
    ornamentalPurpose?: ("exhibition" | "pet" | "historical")[];
}

// Admin SDK initialized by caller

/**
 * Data to be seeded.
 */
const breedStandardsData: BreedStandard[] = [
    // --- CHICKENS ---
    {
        id: "ameraucana",
        name: "Ameraucana",
        origin: "United States",
        species: "Chicken",
        eggColor: "Blue",
        acceptedColors: ["Black", "Blue", "Blue Wheaten", "Brown Red", "Buff", "Silver", "Wheaten", "White", "Lavender"],
        weightRoosterKg: 2.9,
        weightHenKg: 2.5,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 250,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA"],
        geneticProfile: {
            knownGenes: ["O", "pr"],
            baselineGenotype: {
                O_Locus: "O/O",
                P_Locus: "P/p+",
                E_Locus: "e+/e+"
            },
            fixedTraits: ["blue_eggs", "pea_comb", "muffs_beard"],
            inferredTraits: ["E_base", "Wh_base"],
            unknownTraits: ["minor_color_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "rhode_island_red",
        name: "Rhode Island Red",
        origin: "United States",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Red"],
        weightRoosterKg: 3.9,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 280,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["o"],
            baselineGenotype: {
                O_Locus: "o/o",
                E_Locus: "e+/e+"
            },
            fixedTraits: ["brown_eggs", "single_comb", "red_plumage"],
            inferredTraits: ["e+_base"],
            unknownTraits: ["specific_tint_intensity_genes"],
            confidenceLevel: "high"
        }
    },
    {
        id: "ankona",
        name: "Ancona",
        origin: "Italy",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Single Comb", "Rose Comb"],
        weightRoosterKg: 2.7,
        weightHenKg: 2.0,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 220,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["mo"],
            baselineGenotype: {
                O_Locus: "o/o",
                E_Locus: "E/E",
                Mo_Locus: "mo/mo"
            },
            fixedTraits: ["white_eggs", "mottled_pattern", "single_comb"],
            inferredTraits: ["E_base"],
            unknownTraits: ["rose_comb_allele_frequency"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "andalusian",
        name: "Andalusian",
        origin: "Spain",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Blue"],
        weightRoosterKg: 3.2,
        weightHenKg: 2.5,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 150,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Bl"],
            baselineGenotype: {
                Bl_Locus: "Bl/bl+",
                E_Locus: "E/E"
            },
            fixedTraits: ["white_eggs", "blue_plumage", "single_comb"],
            inferredTraits: ["E_base"],
            unknownTraits: ["dilution_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "araucaria",
        name: "Araucana",
        origin: "Chile",
        species: "Chicken",
        eggColor: "Blue",
        acceptedColors: ["Black", "Black Red", "Golden Duckwing", "Silver Duckwing", "White"],
        weightRoosterKg: 2.3,
        weightHenKg: 1.8,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["O", "Rp"],
            baselineGenotype: {
                O_Locus: "O/O",
                Rp_Locus: "Rp/Rp",
                P_Locus: "P/P"
            },
            fixedTraits: ["blue_eggs", "rumpless", "tufted", "pea_comb"],
            inferredTraits: ["lethal_tuft_gene_linkage"],
            unknownTraits: ["exact_base_color_genetics"],
            confidenceLevel: "high"
        }
    },
    {
        id: "australorp",
        name: "Australorp",
        origin: "Australia",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Black", "Blue", "White"],
        weightRoosterKg: 3.9,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 250,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["E"],
            baselineGenotype: {
                O_Locus: "o/o",
                E_Locus: "E/E"
            },
            fixedTraits: ["brown_eggs", "single_comb", "black_plumage"],
            inferredTraits: ["high_production_alleles"],
            unknownTraits: ["eye_color_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "ayam_cemani",
        name: "Ayam Cemani",
        origin: "Indonesia",
        species: "Chicken",
        eggColor: "Cream",
        acceptedColors: ["Black"],
        weightRoosterKg: 2.5,
        weightHenKg: 2.0,
        eggProduction: 80,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["EE"],
        geneticProfile: {
            knownGenes: ["Fm"],
            baselineGenotype: {
                O_Locus: "o/o",
                Fm_Locus: "Fm/Fm",
                E_Locus: "E/E"
            },
            fixedTraits: ["cream_eggs", "fibromelanosis", "black_plumage", "single_comb"],
            inferredTraits: ["E_base"],
            unknownTraits: ["exact_fibromelanosis_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "marans",
        name: "Marans",
        origin: "France",
        species: "Chicken",
        eggColor: "Dark Brown",
        acceptedColors: ["Black Copper", "Cuckoo", "Black", "Wheaten", "White", "Birchen", "Blue Copper", "Golden Cuckoo", "Silver Cuckoo"],
        weightRoosterKg: 3.5,
        weightHenKg: 3.0,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["o", "pr+"],
            baselineGenotype: {
                O_Locus: "o/o",
                Pr_Locus: "pr+/pr+"
            },
            fixedTraits: ["dark_brown_eggs", "single_comb"],
            inferredTraits: ["birchen_base_in_copper_vars"],
            unknownTraits: ["shank_feathering_genetics"],
            confidenceLevel: "high"
        }
    },
    {
        id: "barnevelder",
        name: "Barnevelder",
        origin: "Netherlands",
        species: "Chicken",
        eggColor: "Dark Brown",
        acceptedColors: ["Double Laced", "White", "Black", "Blue Double Laced"],
        weightRoosterKg: 3.5,
        weightHenKg: 2.7,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 180,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Lg"],
            baselineGenotype: {
                O_Locus: "o/o",
                Lg_Locus: "Lg/Lg",
                E_Locus: "eb/eb"
            },
            fixedTraits: ["dark_brown_eggs", "double_laced_pattern", "single_comb"],
            inferredTraits: ["Eb_base"],
            unknownTraits: ["exact_lacing_modifiers"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "bielefelder",
        name: "Bielefelder",
        origin: "Germany",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Crele"],
        weightRoosterKg: 4.5,
        weightHenKg: 3.6,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 230,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["EE", "PCGB"],
        geneticProfile: {
            knownGenes: ["B", "e+"],
            baselineGenotype: {
                B_Locus: "B/b+",
                E_Locus: "e+/e+"
            },
            fixedTraits: ["brown_eggs", "crele_pattern", "single_comb", "auto_sexing"],
            inferredTraits: ["wild_type_base"],
            unknownTraits: ["growth_rate_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "brahma",
        name: "Brahma",
        origin: "United States / China",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Light", "Dark", "Buff"],
        weightRoosterKg: 5.5,
        weightHenKg: 4.3,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 150,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["o", "Co"],
            baselineGenotype: {
                O_Locus: "o/o",
                Co_Locus: "Co/Co",
                P_Locus: "P/P"
            },
            fixedTraits: ["brown_eggs", "pea_comb", "feathered_shanks", "large_size"],
            inferredTraits: ["e+_base"],
            unknownTraits: ["buff_color_intensity_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "buckeye",
        name: "Buckeye",
        origin: "United States",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Mahogany Red"],
        weightRoosterKg: 4.1,
        weightHenKg: 2.9,
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA"],
        geneticProfile: {
            knownGenes: ["P", "Mh"],
            baselineGenotype: {
                P_Locus: "P/P",
                Mh_Locus: "Mh/Mh",
                O_Locus: "o/o",
            },
            fixedTraits: ["pea_comb", "mahogany", "yellow_skin"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "buttercup",
        name: "Sicilian Buttercup",
        origin: "Italy",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Golden Buff"],
        weightRoosterKg: 2.9,
        weightHenKg: 2.3,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 180,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["Bc"],
            baselineGenotype: {
                O_Locus: "o/o",
                Bc_Locus: "Bc/Bc"
            },
            fixedTraits: ["white_eggs", "buttercup_comb"],
            inferredTraits: ["eb_base"],
            unknownTraits: ["comb_stability_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "campine",
        name: "Campine",
        origin: "Belgium",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Silver", "Golden"],
        weightRoosterKg: 2.7,
        weightHenKg: 2.3,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 150,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Ab"],
            baselineGenotype: {
                O_Locus: "o/o",
                Ab_Locus: "Ab/Ab",
                E_Locus: "eb/eb"
            },
            fixedTraits: ["white_eggs", "autosomal_barring", "single_comb"],
            inferredTraits: ["eb_base"],
            unknownTraits: ["hen_feathering_alleles"],
            confidenceLevel: "high"
        }
    },
    {
        id: "chantecler",
        name: "Chantecler",
        origin: "Canada",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["White", "Partridge"],
        weightRoosterKg: 3.9,
        weightHenKg: 2.9,
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA"],
        geneticProfile: {
            knownGenes: ["R"],
            baselineGenotype: {
                R_Locus: "R/R",
                O_Locus: "o/o",
            },
            fixedTraits: ["cushion_comb", "yellow_skin", "white_plumage"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "cochin",
        name: "Cochin",
        origin: "China",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Black", "Buff", "Partridge", "White", "Blue", "Silver Laced", "Golden Laced", "Barred", "Brown Red"],
        weightRoosterKg: 5.0,
        weightHenKg: 3.9,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 120,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Co"],
            baselineGenotype: {
                O_Locus: "o/o",
                Co_Locus: "Co/Co"
            },
            fixedTraits: ["brown_eggs", "single_comb", "feathered_shanks", "heavy_feathering"],
            inferredTraits: ["e+_base"],
            unknownTraits: ["buff_dilution_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "silkie",
        name: "Silkie",
        origin: "China",
        species: "Chicken",
        eggColor: "Cream",
        acceptedColors: ["Black", "Blue", "Buff", "Gray", "Partridge", "White"],
        weightRoosterKg: 1.0,
        weightHenKg: 0.9,
        eggProduction: 100,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["h", "Fm", "P", "R", "Cr", "Po"],
            baselineGenotype: {
                H_Locus: "h/h",
                Fm_Locus: "Fm/Fm",
                P_Locus: "P/P",
                R_Locus: "R/R",
                Cr_Locus: "Cr/Cr",
                Po_Locus: "Po/Po",
                O_Locus: "o/o",
            },
            fixedTraits: ["silkied_feathers", "black_skin", "walnut_comb", "crest", "five_toes"],
            confidenceLevel: "high"
        },
        category: "bantam",
        isTrueBantam: true,
        ornamentalPurpose: ["exhibition", "pet"]
    },
    {
        id: "cornish",
        name: "Cornish",
        origin: "United Kingdom",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Dark", "White", "White Laced Red", "Buff"],
        weightRoosterKg: 4.8,
        weightHenKg: 3.6,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 80,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["P"],
            baselineGenotype: {
                P_Locus: "P/P",
                O_Locus: "o/o",
                W_Locus: "w+/w+",
            },
            fixedTraits: ["pea_comb", "hard_feathering", "broad_breast"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "crevecoeur",
        name: "Crevecoeur",
        origin: "France",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Black"],
        weightRoosterKg: 3.6,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 120,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Cr", "P"],
            baselineGenotype: {
                O_Locus: "o/o",
                Cr_Locus: "Cr/Cr",
                Dv_Locus: "Dv/Dv"
            },
            fixedTraits: ["white_eggs", "v_comb", "crest", "muffs_beard"],
            inferredTraits: ["E_base"],
            unknownTraits: ["exact_crest_stability"],
            confidenceLevel: "high"
        }
    },
    {
        id: "cubalaya",
        name: "Cubalaya",
        origin: "Cuba",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Black Breasted Red", "White", "Black"],
        weightRoosterKg: 2.7,
        weightHenKg: 1.8,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 100,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o",
                P_Locus: "P/P"
            },
            fixedTraits: ["white_eggs", "pea_comb", "lobster_tail"],
            inferredTraits: ["wild_type_base"],
            unknownTraits: ["exact_tail_angle_modifiers"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "delaware",
        name: "Delaware",
        origin: "United States",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["White with Black Barring (Columbian pattern)"],
        weightRoosterKg: 3.9,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA"],
        geneticProfile: {
            knownGenes: ["B", "S"],
            baselineGenotype: {
                O_Locus: "o/o",
                B_Locus: "B/B",
                S_Locus: "S/S",
                Co_Locus: "Co/Co"
            },
            fixedTraits: ["brown_eggs", "single_comb", "barred_pattern", "silver_base"],
            inferredTraits: ["E_base"],
            unknownTraits: ["barring_width_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "dominique",
        name: "Dominique",
        origin: "United States",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Cuckoo (Barred)"],
        weightRoosterKg: 3.2,
        weightHenKg: 2.3,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 230,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA"],
        geneticProfile: {
            knownGenes: ["B"],
            baselineGenotype: {
                O_Locus: "o/o",
                B_Locus: "B/B",
                R_Locus: "R/R"
            },
            fixedTraits: ["brown_eggs", "rose_comb", "cuckoo_pattern"],
            inferredTraits: ["E_base"],
            unknownTraits: ["exact_cuckoo_uniformity"],
            confidenceLevel: "high"
        }
    },
    {
        id: "dorking",
        name: "Dorking",
        origin: "United Kingdom / Italy",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["White", "Silver-Gray", "Colored", "Red", "Cuckoo"],
        weightRoosterKg: 4.1,
        weightHenKg: 3.2,
        size: "Medium",
        weightClass: "Heavy",
        eggProduction: 140,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o",
                Po_Locus: "Po/Po"
            },
            fixedTraits: ["white_eggs", "single_comb", "five_toes", "large_size"],
            inferredTraits: ["red_base_in_colored_var"],
            unknownTraits: ["extra_toe_stability"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "faverolles",
        name: "Faverolles",
        origin: "France",
        species: "Chicken",
        eggColor: "Light Brown",
        acceptedColors: ["Salmon", "White"],
        weightRoosterKg: 3.6,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 180,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o",
                Po_Locus: "Po/Po",
                Mb_Locus: "Mb/Mb"
            },
            fixedTraits: ["light_brown_eggs", "single_comb", "muffs_beard", "five_toes"],
            inferredTraits: ["wild_type_variation"],
            unknownTraits: ["salmon_color_modifiers"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "hamburg",
        name: "Hamburg",
        origin: "Germany / Netherlands",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Silver Spangled", "Golden Spangled", "Golden Penciled", "Silver Penciled", "White", "Black"],
        weightRoosterKg: 2.3,
        weightHenKg: 1.8,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Sp"],
            baselineGenotype: {
                O_Locus: "o/o",
                Sp_Locus: "Sp/Sp",
                R_Locus: "R/R"
            },
            fixedTraits: ["white_eggs", "rose_comb", "spangled_pattern", "white_earlobes"],
            inferredTraits: ["E_base"],
            unknownTraits: ["exact_spangling_symmetry"],
            confidenceLevel: "high"
        }
    },
    {
        id: "houdan",
        name: "Houdan",
        origin: "France",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Mottled", "White"],
        weightRoosterKg: 3.6,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 150,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Cr"],
            baselineGenotype: {
                O_Locus: "o/o",
                Cr_Locus: "Cr/Cr",
                Dv_Locus: "Dv/Dv"
            },
            fixedTraits: ["white_eggs", "v_comb", "crest", "mottled_pattern", "five_toes"],
            inferredTraits: ["E_base"],
            unknownTraits: ["crest_mottling_interaction"],
            confidenceLevel: "high"
        }
    },
    {
        id: "jersey_giant",
        name: "Jersey Giant",
        origin: "United States",
        species: "Chicken",
        eggColor: "Dark Brown",
        acceptedColors: ["Black", "White", "Blue"],
        weightRoosterKg: 5.9,
        weightHenKg: 4.5,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 180,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o",
                E_Locus: "E/E",
                Id_Locus: "id+/id+"
            },
            fixedTraits: ["dark_brown_eggs", "single_comb", "enormous_size"],
            inferredTraits: ["E_base"],
            unknownTraits: ["giantism_modifiers"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "la_fleche",
        name: "La Flèche",
        origin: "France",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Black"],
        weightRoosterKg: 3.6,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 180,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Cr", "P"],
            baselineGenotype: {
                O_Locus: "o/o",
                Dv_Locus: "Dv/Dv"
            },
            fixedTraits: ["white_eggs", "v_comb", "crest"],
            inferredTraits: ["E_base"],
            unknownTraits: ["horn_comb_extension"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "lakenvelder",
        name: "Lakenvelder",
        origin: "Germany / Netherlands",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Black and White"],
        weightRoosterKg: 2.3,
        weightHenKg: 1.8,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 160,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Co"],
            baselineGenotype: {
                O_Locus: "o/o",
                Co_Locus: "Co/Co"
            },
            fixedTraits: ["white_eggs", "single_comb", "belted_pattern"],
            inferredTraits: ["e+_base"],
            unknownTraits: ["belt_uniformity_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "langshan_croad",
        name: "Croad Langshan",
        origin: "China",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Black", "White"],
        weightRoosterKg: 4.3,
        weightHenKg: 3.4,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 150,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o"
            },
            fixedTraits: ["brown_eggs", "single_comb", "feathered_shanks"],
            inferredTraits: ["E_base"],
            unknownTraits: ["egg_shade_depth"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "legbar",
        name: "Cream Legbar",
        origin: "United Kingdom",
        species: "Chicken",
        eggColor: "Blue/Green",
        acceptedColors: ["Cream", "Golden", "Silver"],
        weightRoosterKg: 3.2,
        weightHenKg: 2.5,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 180,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["PCGB"],
        geneticProfile: {
            knownGenes: ["O", "B"],
            baselineGenotype: {
                O_Locus: "O/O",
                B_Locus: "B/B",
                Cr_Locus: "Cr/Cr"
            },
            fixedTraits: ["blue_eggs", "auto_sexing", "crested"],
            inferredTraits: ["brown_base_shell"],
            unknownTraits: ["exact_feather_color_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "leghorn",
        name: "Leghorn",
        origin: "Italy",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Single Comb White", "Rose Comb White", "Single Comb Light Brown", "Single Comb Dark Brown", "Single Comb Black", "Single Comb Buff", "Single Comb Silver", "Single Comb Red Pyle", "Single Comb Columbian"],
        weightRoosterKg: 3.4,
        weightHenKg: 2.5,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 280,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["I"],
            baselineGenotype: {
                O_Locus: "o/o",
                I_Locus: "I/I",
                E_Locus: "E/E"
            },
            fixedTraits: ["white_eggs", "single_comb", "white_plumage", "yellow_skin"],
            inferredTraits: ["high_production_alleles"],
            unknownTraits: ["black_base_leakage"],
            confidenceLevel: "high"
        }
    },

    {
        id: "malay",
        name: "Malay",
        origin: "Asia",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Black Breasted Red", "Spangled", "Black", "White", "Red Pyle"],
        weightRoosterKg: 4.1,
        weightHenKg: 3.2,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 50,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o"
            },
            fixedTraits: ["brown_eggs", "strawberry_comb", "extreme_height"],
            inferredTraits: ["game_ancestry"],
            unknownTraits: ["height_modifiers"],
            confidenceLevel: "low"
        }
    },
    {
        id: "minorca",
        name: "Minorca",
        origin: "Spain",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Black", "White", "Buff"],
        weightRoosterKg: 4.1,
        weightHenKg: 3.4,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 140,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o"
            },
            fixedTraits: ["white_eggs", "single_comb", "large_white_earlobes"],
            inferredTraits: ["E_base"],
            unknownTraits: ["earlobe_size_modifiers"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "modern_game",
        name: "Modern Game",
        origin: "United Kingdom",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Black Breasted Red", "Brown Red", "Golden Duckwing", "Silver Duckwing", "Birchen", "Red Pyle", "White", "Black"],
        weightRoosterKg: 2.7,
        weightHenKg: 2.0,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 50,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o"
            },
            fixedTraits: ["white_eggs", "single_comb", "extreme_reachy_posture"],
            inferredTraits: ["E_base"],
            unknownTraits: ["posture_genetics"],
            confidenceLevel: "low"
        }
    },
    {
        id: "naked_neck",
        name: "Naked Neck (Turken)",
        origin: "Transylvania",
        species: "Chicken",
        eggColor: "Light Brown",
        acceptedColors: ["Red", "White", "Buff", "Black"],
        weightRoosterKg: 3.9,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 180,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "EE"],
        geneticProfile: {
            knownGenes: ["Na"],
            baselineGenotype: {
                O_Locus: "o/o",
                Na_Locus: "Na/Na"
            },
            fixedTraits: ["light_brown_eggs", "naked_neck", "single_comb"],
            inferredTraits: ["E_base"],
            unknownTraits: ["neck_feathering_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "new_hampshire_red",
        name: "New Hampshire",
        origin: "United States",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Chestnut Red"],
        weightRoosterKg: 3.9,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o",
                E_Locus: "e+/e+"
            },
            fixedTraits: ["brown_eggs", "single_comb", "chestnut_red_plumage"],
            inferredTraits: ["e+_base"],
            unknownTraits: ["red_intensity_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "old_english_game",
        name: "Old English Game",
        origin: "United Kingdom",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Black Breasted Red", "Brown Red", "Golden Duckwing", "Silver Duckwing", "Red Pyle", "White", "Black", "Spangled", "Blue Breasted Red", "Lemon Blue", "Blue Golden Duckwing", "Blue Silver Duckwing", "Self Blue", "Crele"],
        weightRoosterKg: 2.3,
        weightHenKg: 1.8,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 100,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                P_Locus: "p+/p+",
                O_Locus: "o/o",
            },
            fixedTraits: ["single_comb", "white_skin", "game_posture"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "orpington",
        name: "Orpington",
        origin: "United Kingdom",
        species: "Chicken",
        eggColor: "Light Brown",
        acceptedColors: ["Buff", "Black", "White", "Blue"],
        weightRoosterKg: 4.5,
        weightHenKg: 3.6,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 180,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Co"],
            baselineGenotype: {
                O_Locus: "o/o"
            },
            fixedTraits: ["light_brown_eggs", "single_comb", "heavy_feathering"],
            inferredTraits: ["E_base", "e+_base"],
            unknownTraits: ["buff_dilution_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "phoenix",
        name: "Phoenix",
        origin: "Germany",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Silver", "Golden"],
        weightRoosterKg: 2.3,
        weightHenKg: 1.8,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 80,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "EE"],
        category: "ornamental",
        ornamentalPurpose: ["exhibition", "historical"],
        geneticProfile: {
            knownGenes: ["Gt"],
            baselineGenotype: {
                O_Locus: "o/o"
            },
            fixedTraits: ["white_eggs", "single_comb", "extremely_long_tail"],
            inferredTraits: ["wild_type_base"],
            unknownTraits: ["tail_length_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "plymouth_rock",
        name: "Plymouth Rock",
        origin: "United States",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Barred", "White", "Buff", "Silver Penciled", "Partridge", "Columbian", "Blue"],
        weightRoosterKg: 4.3,
        weightHenKg: 3.4,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["B"],
            baselineGenotype: {
                B_Locus: "B/B",
                E_Locus: "E/E"
            },
            fixedTraits: ["brown_eggs", "single_comb", "clean_legs"],
            inferredTraits: ["E_base"],
            unknownTraits: ["barring_width_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "polish",
        name: "Polish",
        origin: "Europe",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["White Crested Black", "Non-Bearded Golden", "Non-Bearded Silver", "Non-Bearded White", "Non-Bearded Buff Laced", "Bearded Golden", "Bearded Silver", "Bearded White", "Bearded Buff Laced"],
        weightRoosterKg: 2.7,
        weightHenKg: 2.0,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 120,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        category: "ornamental",
        ornamentalPurpose: ["exhibition"],
        geneticProfile: {
            knownGenes: ["Cr"],
            baselineGenotype: {
                O_Locus: "o/o",
                Cr_Locus: "Cr/Cr",
                Dv_Locus: "Dv/Dv"
            },
            fixedTraits: ["white_eggs", "v_comb", "large_crest"],
            inferredTraits: ["E_base"],
            unknownTraits: ["crest_size_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "rhode_island_white",
        name: "Rhode Island White",
        origin: "United States",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Rose Comb"],
        weightRoosterKg: 3.9,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 220,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA"],
        geneticProfile: {
            knownGenes: ["I"],
            baselineGenotype: {
                O_Locus: "o/o",
                I_Locus: "I/I",
                R_Locus: "R/R"
            },
            fixedTraits: ["brown_eggs", "rose_comb", "white_plumage"],
            inferredTraits: ["dominant_white", "e+_base"],
            unknownTraits: ["specific_tint_intensity"],
            confidenceLevel: "high"
        }
    },
    {
        id: "rosecomb",
        name: "Rosecomb",
        origin: "United Kingdom",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Black", "White", "Blue"],
        weightRoosterKg: 0.62,
        weightHenKg: 0.51,
        size: "Small",
        weightClass: "Light",
        eggProduction: 50,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        category: "bantam",
        isTrueBantam: true,
        ornamentalPurpose: ["exhibition"],
        geneticProfile: {
            knownGenes: ["R"],
            baselineGenotype: {
                O_Locus: "o/o",
                R_Locus: "R/R"
            },
            fixedTraits: ["white_eggs", "rose_comb", "large_white_earlobes"],
            inferredTraits: ["E_base"],
            unknownTraits: ["earlobe_shape_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "sebright",
        name: "Sebright",
        origin: "United Kingdom",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Golden", "Silver"],
        weightRoosterKg: 0.62,
        weightHenKg: 0.51,
        size: "Small",
        weightClass: "Light",
        eggProduction: 50,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        category: "bantam",
        isTrueBantam: true,
        ornamentalPurpose: ["exhibition"],
        geneticProfile: {
            knownGenes: ["Hf", "Lg"],
            baselineGenotype: {
                O_Locus: "o/o",
                Hf_Locus: "Hf/Hf",
                Lg_Locus: "Lg/Lg",
                R_Locus: "R/R"
            },
            fixedTraits: ["white_eggs", "rose_comb", "hen_feathering", "laced_pattern"],
            inferredTraits: ["eb_base"],
            unknownTraits: ["lacing_perfection_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "spanish",
        name: "White-Faced Black Spanish",
        origin: "Spain",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Black"],
        weightRoosterKg: 3.6,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 160,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o"
            },
            fixedTraits: ["white_eggs", "single_comb", "large_white_face"],
            inferredTraits: ["E_base"],
            unknownTraits: ["face_enlargement_genetics"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "sussex",
        name: "Sussex",
        origin: "United Kingdom",
        species: "Chicken",
        eggColor: "Light Brown",
        acceptedColors: ["Speckled", "Red", "Light", "Coronation", "Brown", "Buff", "White", "Silver"],
        weightRoosterKg: 4.1,
        weightHenKg: 3.2,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Co", "mo"],
            baselineGenotype: {
                O_Locus: "o/o"
            },
            fixedTraits: ["light_brown_eggs", "single_comb", "clean_legs"],
            inferredTraits: ["eb_base", "e+_base"],
            unknownTraits: ["mottling_uniformity"],
            confidenceLevel: "high"
        }
    },
    {
        id: "sumatra",
        name: "Sumatra",
        origin: "Indonesia",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Black", "Blue"],
        weightRoosterKg: 2.3,
        weightHenKg: 1.8,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 100,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["Fm"],
            baselineGenotype: {
                O_Locus: "o/o",
                Fm_Locus: "Fm/Fm",
                P_Locus: "P/P"
            },
            fixedTraits: ["white_eggs", "pea_comb", "black_skin", "multiple_spurs"],
            inferredTraits: ["wild_type_variation"],
            unknownTraits: ["multiple_spurs_genetics"],
            confidenceLevel: "high"
        }
    },
    {
        id: "welsummer",
        name: "Welsummer",
        origin: "Netherlands",
        species: "Chicken",
        eggColor: "Dark Brown",
        acceptedColors: ["Red Partridge"],
        weightRoosterKg: 3.2,
        weightHenKg: 2.7,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 160,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["o", "pr+"],
            baselineGenotype: {
                O_Locus: "o/o"
            },
            fixedTraits: ["dark_brown_eggs", "single_comb", "partridge_pattern"],
            inferredTraits: ["eb_base"],
            unknownTraits: ["egg_speckling_genes"],
            confidenceLevel: "high"
        }
    },
    {
        id: "wyandotte",
        name: "Wyandotte",
        origin: "United States",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Silver Laced", "Golden Laced", "White", "Black", "Buff", "Partridge", "Silver Penciled", "Columbian", "Blue"],
        weightRoosterKg: 3.9,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB", "EE"],
        geneticProfile: {
            knownGenes: ["R", "Lg"],
            baselineGenotype: {
                O_Locus: "o/o",
                R_Locus: "R/R",
                Lg_Locus: "Lg/Lg"
            },
            fixedTraits: ["brown_eggs", "rose_comb", "clean_legs", "curvy_shape"],
            inferredTraits: ["eb_base"],
            unknownTraits: ["lacing_intensity_modifiers"],
            confidenceLevel: "high"
        }
    },

    {
        id: "dutch_bantam",
        name: "Dutch Bantam",
        origin: "Netherlands",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["Light Brown", "Silver", "White", "Black", "Blue"],
        weightRoosterKg: 0.52,
        weightHenKg: 0.43,
        size: "Small",
        weightClass: "Light",
        eggProduction: 80,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["PCGB", "EE", "ABA", "APA"],
        category: "bantam",
        isTrueBantam: true,
        ornamentalPurpose: ["exhibition"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                P_Locus: "p+/p+",
                O_Locus: "o/o",
            },
            fixedTraits: ["single_comb", "white_earlobes", "clean_legged"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "brahma_bantam",
        name: "Brahma Bantam",
        origin: "United States",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Light", "Dark", "Buff"],
        weightRoosterKg: 1.7,
        weightHenKg: 1.5,
        size: "Small",
        weightClass: "Light",
        eggProduction: 80,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB"],
        category: "bantam",
        isTrueBantam: false,
        geneticProfile: {
            knownGenes: ["P", "Pti-1"],
            baselineGenotype: {
                P_Locus: "P/P",
                O_Locus: "o/o",
                Pti_Locus: "Pti-1/Pti-1",
            },
            fixedTraits: ["pea_comb", "feathered_legs", "brown_eggs"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "leghorn_bantam",
        name: "Leghorn Bantam",
        origin: "Italy / United States",
        species: "Chicken",
        eggColor: "White",
        acceptedColors: ["White", "Brown", "Black", "Buff", "Silver"],
        weightRoosterKg: 0.8,
        weightHenKg: 0.7,
        size: "Small",
        weightClass: "Light",
        eggProduction: 120,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB"],
        category: "bantam",
        isTrueBantam: false,
        geneticProfile: {
            knownGenes: ["I"],
            baselineGenotype: {
                I_Locus: "I/I",
                O_Locus: "o/o",
            },
            fixedTraits: ["single_comb", "white_earlobes", "white_plumage"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "plymouth_rock_bantam",
        name: "Plymouth Rock Bantam",
        origin: "United States",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Barred", "White", "Buff", "Partridge", "Silver Penciled", "Columbian", "Blue"],
        weightRoosterKg: 1.0,
        weightHenKg: 0.9,
        size: "Small",
        weightClass: "Light",
        eggProduction: 100,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "ABA", "PCGB"],
        category: "bantam",
        isTrueBantam: false,
        geneticProfile: {
            knownGenes: ["B"],
            baselineGenotype: {
                B_Locus: "B/B",
                O_Locus: "o/o",
            },
            fixedTraits: ["single_comb", "yellow_skin", "barred_plumage"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "yokohama",
        name: "Yokohama",
        origin: "Japan / Germany",
        species: "Chicken",
        eggColor: "Cream",
        acceptedColors: ["White", "Red Pyle"],
        weightRoosterKg: 2.0,
        weightHenKg: 1.6,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 80,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB", "EE"],
        category: "ornamental",
        ornamentalPurpose: ["exhibition", "historical"],
        geneticProfile: {
            knownGenes: ["P", "R", "Gt"],
            baselineGenotype: {
                P_Locus: "P/P",
                R_Locus: "R/R",
                Gt_Locus: "Gt/Gt",
                O_Locus: "o/o",
            },
            fixedTraits: ["walnut_comb", "long_tail", "red_saddle"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "onagadori",
        name: "Onagadori",
        origin: "Japan",
        species: "Chicken",
        eggColor: "Light Brown",
        acceptedColors: ["Black Breasted Silver", "Black Breasted Golden", "White", "Black Breasted Red"],
        weightRoosterKg: 1.8,
        weightHenKg: 1.3,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 60,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["EE"],
        category: "ornamental",
        ornamentalPurpose: ["exhibition", "historical", "pet"],
        geneticProfile: {
            knownGenes: ["Gt", "nm"],
            baselineGenotype: {
                Gt_Locus: "Gt/Gt",
                nm_Locus: "nm/nm",
                O_Locus: "o/o",
            },
            fixedTraits: ["single_comb", "non_molting_tail", "long_tail"],
            confidenceLevel: "low"
        }
    },
    // --- DUCKS ---
    {
        id: "aylesbury",
        name: "Aylesbury",
        origin: "United Kingdom",
        species: "Duck",
        eggColor: "White/Green",
        acceptedColors: ["White"],
        weightRoosterKg: 4.5,
        weightHenKg: 4.1,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 100,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["c"],
            baselineGenotype: {
                DUCK__C_Locus: "c+/c+"
            },
            fixedTraits: ["white_green_eggs", "white_plumage", "pale_bill"],
            inferredTraits: ["mallard_derived"],
            unknownTraits: ["egg_color_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "call_duck",
        name: "Call",
        origin: "Unknown",
        species: "Duck",
        eggColor: "White/Blue/Green",
        acceptedColors: ["White", "Gray", "Blue", "Buff", "Pastel", "Snowy"],
        weightRoosterKg: 0.7,
        weightHenKg: 0.6,
        size: "Small",
        weightClass: "Light",
        eggProduction: 80,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["Dw"],
            baselineGenotype: {
                DUCK__Dw_Locus: "Dw/Dw"
            },
            fixedTraits: ["variable_egg_color", "extremely_small_size", "short_bill"],
            inferredTraits: ["mallard_derived"],
            unknownTraits: ["size_reduction_genetics"],
            confidenceLevel: "high"
        }
    },
    {
        id: "cayuga",
        name: "Cayuga",
        origin: "United States",
        species: "Duck",
        eggColor: "Black/Gray/White",
        acceptedColors: ["Black"],
        weightRoosterKg: 3.6,
        weightHenKg: 3.2,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 130,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["E"],
            baselineGenotype: {
                DUCK__E_Locus: "E/E"
            },
            fixedTraits: ["black_grey_eggs", "black_plumage", "green_sheen"],
            inferredTraits: ["mallard_derived"],
            unknownTraits: ["egg_pigment_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "indian_runner",
        name: "Indian Runner",
        origin: "East Indies",
        species: "Duck",
        eggColor: "White/Blue",
        acceptedColors: ["White", "Fawn & White", "Penciled", "Black", "Buff", "Chocolate", "Cumberland Blue", "Gray"],
        weightRoosterKg: 2.0,
        weightHenKg: 1.8,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 300,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["M"],
            baselineGenotype: {
                DUCK__M_Locus: "M/M"
            },
            fixedTraits: ["white_blue_eggs", "upright_posture", "high_production"],
            inferredTraits: ["mallard_derived"],
            unknownTraits: ["posture_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "welsh_harlequin",
        name: "Welsh Harlequin",
        origin: "United Kingdom",
        species: "Duck",
        eggColor: "White",
        acceptedColors: ["Silver"],
        weightRoosterKg: 2.5,
        weightHenKg: 2.3,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 250,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["d"],
            baselineGenotype: {
                DUCK__D_Locus: "d+/d+"
            },
            fixedTraits: ["white_eggs", "harlequin_pattern", "high_production"],
            inferredTraits: ["mallard_derived"],
            unknownTraits: ["pattern_stability_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "khaki_campbell",
        name: "Khaki Campbell",
        origin: "United Kingdom",
        species: "Duck",
        eggColor: "White",
        acceptedColors: ["Khaki"],
        weightRoosterKg: 2.0,
        weightHenKg: 1.8,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 300,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["d"],
            baselineGenotype: {
                DUCK__D_Locus: "d+/d+"
            },
            fixedTraits: ["white_eggs", "khaki_plumage", "high_egg_production"],
            inferredTraits: ["mallard_derived"],
            unknownTraits: ["production_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "muscovy",
        name: "Muscovy",
        origin: "South America",
        species: "Duck",
        eggColor: "White",
        acceptedColors: ["Black", "Blue", "Chocolate", "White", "Buff"],
        weightRoosterKg: 5.5,
        weightHenKg: 3.2,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 100,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["P"],
            baselineGenotype: {
                DUCK__P_Locus: "P/P"
            },
            fixedTraits: ["white_eggs", "caruncles", "claws", "silent"],
            inferredTraits: ["distinct_species_genetics"],
            unknownTraits: ["exact_muscovy_markers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "pekin",
        name: "Pekin",
        origin: "China",
        species: "Duck",
        eggColor: "White",
        acceptedColors: ["White"],
        weightRoosterKg: 4.5,
        weightHenKg: 4.1,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 200,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["c"],
            baselineGenotype: {
                DUCK__C_Locus: "c+/c+"
            },
            fixedTraits: ["white_eggs", "white_plumage", "orange_bill"],
            inferredTraits: ["mallard_derived"],
            unknownTraits: ["growth_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "rouen",
        name: "Rouen",
        origin: "France",
        species: "Duck",
        eggColor: "White/Blue/Green",
        acceptedColors: ["Gray"],
        weightRoosterKg: 4.5,
        weightHenKg: 3.6,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 100,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["M"],
            baselineGenotype: {
                DUCK__M_Locus: "M/M"
            },
            fixedTraits: ["variable_egg_color", "mallard_pattern", "heavy_weight"],
            inferredTraits: ["mallard_derived"],
            unknownTraits: ["weight_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "swedish_blue_duck",
        name: "Swedish Blue",
        origin: "Sweden (Pomerania)",
        species: "Duck",
        eggColor: "White/Blue/Green",
        acceptedColors: ["Blue"],
        weightRoosterKg: 3.6,
        weightHenKg: 2.9,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 100,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["Bl"],
            baselineGenotype: {
                DUCK__Bl_Locus: "Bl/bl+"
            },
            fixedTraits: ["variable_egg_color", "blue_plumage", "white_bib"],
            inferredTraits: ["mallard_derived"],
            unknownTraits: ["bib_shape_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "merchtemse_eend",
        name: "Merchtemse Eend",
        origin: "Belgium",
        species: "Duck",
        eggColor: "White",
        acceptedColors: ["White"],
        weightRoosterKg: 2.25,
        weightHenKg: 2.0,
        size: "Medium",
        weightClass: "Light",
        eggProduction: 80,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["EE"],
        geneticProfile: {
            knownGenes: ["c", "M"],
            baselineGenotype: {
                DUCK__C_Locus: "c+/c+",
                DUCK__M_Locus: "M/M",
            },
            fixedTraits: ["white_plumage", "blue_bill", "white_eggs"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "dendermonde_duck",
        name: "Dendermondse Eend",
        origin: "Belgium",
        species: "Duck",
        eggColor: "Greenish",
        acceptedColors: ["Blue", "Black"],
        weightRoosterKg: 2.75,
        weightHenKg: 2.5,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 80,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["EE"],
        geneticProfile: {
            knownGenes: ["Bl", "M"],
            baselineGenotype: {
                DUCK__Bl_Locus: "Bl/bl+",
                DUCK__M_Locus: "M/M",
            },
            fixedTraits: ["blue_plumage", "white_bib", "greenish_eggs"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "mandarin",
        name: "Mandarin Duck",
        origin: "East Asia",
        species: "Duck",
        eggColor: "Cream/White",
        acceptedColors: ["Wild Type", "White"],
        weightRoosterKg: 0.6,
        weightHenKg: 0.5,
        size: "Small",
        weightClass: "Light",
        eggProduction: 60,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                SPECIES_Specific: "Aix_galericulata",
            },
            fixedTraits: ["ornamental_plumage", "perching_behavior", "cream_eggs"],
            confidenceLevel: "low"
        }
    },
    {
        id: "wood_duck",
        name: "Wood Duck",
        origin: "North America",
        species: "Duck",
        eggColor: "Cream/White",
        acceptedColors: ["Wild Type", "White", "Silver"],
        weightRoosterKg: 0.7,
        weightHenKg: 0.6,
        size: "Small",
        weightClass: "Light",
        eggProduction: 80,
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                SPECIES_Specific: "Aix_sponsa",
            },
            fixedTraits: ["ornamental_plumage", "perching_behavior", "cream_eggs"],
            confidenceLevel: "low"
        }
    },


    // --- GEESE ---
    {
        id: "african_goose",
        name: "African",
        origin: "China",
        species: "Goose",
        eggColor: "White",
        acceptedColors: ["Brown", "White"],
        weightRoosterKg: 9.1,
        weightHenKg: 8.2,
        size: "Large",
        weightClass: "Heavy",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["Kn", "De"],
            baselineGenotype: {
                GOOSE__Kn_Locus: "Kn/Kn",
                GOOSE__De_Locus: "De/De",
            },
            fixedTraits: ["knob", "dewlap", "brown_stripe"],
            confidenceLevel: "high"
        }
    },
    {
        id: "chinese_goose",
        name: "Chinese",
        origin: "China",
        species: "Goose",
        eggColor: "White",
        acceptedColors: ["Brown", "White"],
        weightRoosterKg: 5.4,
        weightHenKg: 4.5,
        size: "Medium",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["Kn"],
            baselineGenotype: {
                GOOSE__Kn_Locus: "Kn/Kn",
            },
            fixedTraits: ["knob", "slender_neck", "high_egg_production"],
            confidenceLevel: "high"
        }
    },
    {
        id: "embden",
        name: "Embden",
        origin: "Germany",
        species: "Goose",
        eggColor: "White",
        acceptedColors: ["White"],
        weightRoosterKg: 12.0,
        weightHenKg: 9.0,
        size: "Large",
        weightClass: "Heavy",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                GOOSE__C_Locus: "c/c",
            },
            fixedTraits: ["white_plumage", "blue_eyes", "orange_bill"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "pilgrim",
        name: "Pilgrim",
        origin: "United States",
        species: "Goose",
        eggColor: "White",
        acceptedColors: ["Sex-linked pattern"],
        weightRoosterKg: 6.3,
        weightHenKg: 5.4,
        size: "Large",
        weightClass: "Medium",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["Sd"],
            baselineGenotype: {
                GOOSE__Sd_Locus: "Sd/sd+",
            },
            fixedTraits: ["sex_linked_dimorphism", "white_gander_grey_goose", "medium_size"],
            confidenceLevel: "high"
        }
    },
    {
        id: "sebastopol",
        name: "Sebastopol",
        origin: "Europe (Near Danube)",
        species: "Goose",
        eggColor: "White",
        acceptedColors: ["White"],
        weightRoosterKg: 6.3,
        weightHenKg: 5.4,
        size: "Large",
        weightClass: "Medium",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["L"],
            baselineGenotype: {
                GOOSE__L_Locus: "L/L",
            },
            fixedTraits: ["curled_feathers", "white_plumage", "unable_to_fly"],
            confidenceLevel: "high"
        }
    },
    {
        id: "toulouse",
        name: "Toulouse",
        origin: "France",
        species: "Goose",
        eggColor: "White",
        acceptedColors: ["Gray", "Buff"],
        weightRoosterKg: 12.0,
        weightHenKg: 9.0,
        size: "Large",
        weightClass: "Heavy",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                GOOSE__Gy_Locus: "Gy/Gy",
            },
            fixedTraits: ["grey_plumage", "dewlap", "heavy_weight"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "american_buff_goose",
        name: "American Buff",
        origin: "United States",
        species: "Goose",
        eggColor: "White",
        acceptedColors: ["Buff"],
        weightRoosterKg: 8.0,
        weightHenKg: 7.0,
        size: "Medium",
        weightClass: "Medium",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["Bu"],
            baselineGenotype: {
                GOOSE__Bu_Locus: "Bu/Bu",
            },
            fixedTraits: ["buff_plumage", "orange_bill", "calm_temperament"],
            confidenceLevel: "medium"
        }
    },
    // --- CROSS-BREEDS / HYBRIDS ---
    {
        id: "olive_egger",
        name: "Olive Egger",
        origin: "Crossbreed",
        species: "Chicken",
        eggColor: "Olive",
        acceptedColors: ["Variable", "Black", "Blue", "Splash"],
        weightRoosterKg: 3.0,
        weightHenKg: 2.5,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 200,
        official: false,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: [],
        geneticProfile: {
            knownGenes: ["O", "pr+"], // Blue egg + Dark Brown pigment
            baselineGenotype: {
                O_Locus: "O/o",
                Pr_Locus: "pr+/pr+"
            },
            fixedTraits: ["olive_eggs", "pea_comb_likely", "muffs_beard_likely"],
            inferredTraits: ["complex_hybrid"],
            unknownTraits: ["exact_f1_or_f2_status"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "easter_egger",
        name: "Easter Egger",
        origin: "Crossbreed",
        species: "Chicken",
        eggColor: "Variable (Blue/Green/Pink)",
        acceptedColors: ["Variable"],
        weightRoosterKg: 2.7,
        weightHenKg: 2.3,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 200,
        official: false,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: [],
        geneticProfile: {
            knownGenes: ["O"],
            baselineGenotype: {
                O_Locus: "O/o"
            },
            fixedTraits: ["colored_eggs", "muffs_beard_likely", "pea_comb_likely"],
            inferredTraits: ["ameraucana_ancestry"],
            unknownTraits: ["egg_color_shade"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "black_sex_link",
        name: "Black Sex-Link",
        origin: "Hybrid",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Black with Gold/Red leakage (Hens)", "Barred (Roosters)"],
        weightRoosterKg: 3.5,
        weightHenKg: 2.8,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 280,
        official: false,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: [],
        geneticProfile: {
            knownGenes: ["B", "S", "s+"],
            baselineGenotype: {
                B_Locus: "B/b+",
                S_Locus: "S/s+"
            },
            fixedTraits: ["brown_eggs", "sex_linked_plumage", "high_production"],
            inferredTraits: ["rir_x_barred_rock"],
            unknownTraits: [],
            confidenceLevel: "high"
        }
    },
    {
        id: "golden_comet",
        name: "Golden Comet",
        origin: "Hybrid",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["Red/White (Hens)", "White (Roosters)"],
        weightRoosterKg: 2.8,
        weightHenKg: 2.2,
        size: "Medium",
        weightClass: "Medium",
        eggProduction: 300,
        official: false,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: [],
        geneticProfile: {
            knownGenes: ["s+", "S"],
            baselineGenotype: {
                O_Locus: "o/o",
                S_Locus: "S/s+"
            },
            fixedTraits: ["brown_eggs", "sex_linked_plumage", "extreme_production"],
            inferredTraits: ["rir_x_white_rock"],
            unknownTraits: [],
            confidenceLevel: "high"
        }
    },
    {
        id: "cornish_cross",
        name: "Cornish Cross",
        origin: "Hybrid",
        species: "Chicken",
        eggColor: "Brown",
        acceptedColors: ["White"],
        weightRoosterKg: 4.5, // Meat bird weight at maturity
        weightHenKg: 3.5,
        size: "Large",
        weightClass: "Heavy",
        eggProduction: 150,
        official: false,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: [],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                O_Locus: "o/o"
            },
            fixedTraits: ["rapid_growth", "heavy_muscling", "white_plumage"],
            inferredTraits: ["cornish_x_white_rock"],
            unknownTraits: ["metabolic_issues"],
            confidenceLevel: "high"
        }
    },
    {
        id: "black_turkey",
        name: "Black",
        origin: "Europe",
        species: "Turkey",
        eggColor: "Spotted",
        acceptedColors: ["Black"],
        weightRoosterKg: 15.0,
        weightHenKg: 8.0,
        size: "Large",
        weightClass: "Heavy",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["B"],
            baselineGenotype: {
                TURKEY__B_Locus: "B/B"
            },
            fixedTraits: ["spotted_eggs", "black_plumage"],
            inferredTraits: ["meleagris_gallopavo_derived"],
            unknownTraits: ["egg_spotting_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "bourbon_red",
        name: "Bourbon Red",
        origin: "United States",
        species: "Turkey",
        eggColor: "Spotted",
        acceptedColors: ["Red"],
        weightRoosterKg: 15.0,
        weightHenKg: 8.0,
        size: "Large",
        weightClass: "Heavy",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA"],
        geneticProfile: {
            knownGenes: ["r"],
            baselineGenotype: {
                TURKEY__R_Locus: "r/r"
            },
            fixedTraits: ["spotted_eggs", "bourbon_red_plumage", "white_flight_feathers"],
            inferredTraits: ["meleagris_gallopavo_derived"],
            unknownTraits: ["red_depth_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "bronze_turkey",
        name: "Bronze",
        origin: "North America",
        species: "Turkey",
        eggColor: "Spotted",
        acceptedColors: ["Bronze"],
        weightRoosterKg: 16.0,
        weightHenKg: 9.0,
        size: "Large",
        weightClass: "Heavy",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA", "PCGB"],
        geneticProfile: {
            knownGenes: ["b+"],
            baselineGenotype: {
                TURKEY__b_Locus: "b+/b+",
            },
            fixedTraits: ["bronze_plumage", "iridescent", "large_size"],
            confidenceLevel: "high"
        }
    },
    {
        id: "narragansett_turkey",
        name: "Narragansett",
        origin: "United States",
        species: "Turkey",
        eggColor: "Spotted",
        acceptedColors: ["Steel Gray / Black"],
        weightRoosterKg: 15.0,
        weightHenKg: 8.0,
        size: "Large",
        weightClass: "Heavy",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA"],
        geneticProfile: {
            knownGenes: ["n"],
            baselineGenotype: {
                TURKEY__N_Locus: "n/n"
            },
            fixedTraits: ["spotted_eggs", "steel_grey_pattern", "black_markings"],
            inferredTraits: ["meleagris_gallopavo_derived"],
            unknownTraits: ["grey_clarity_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "royal_palm",
        name: "Royal Palm",
        origin: "United States",
        species: "Turkey",
        eggColor: "Spotted",
        acceptedColors: ["White with Black edging"],
        weightRoosterKg: 10.0,
        weightHenKg: 5.5,
        size: "Medium",
        weightClass: "Medium",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA"],
        geneticProfile: {
            knownGenes: ["p"],
            baselineGenotype: {
                TURKEY__P_Locus: "p/p"
            },
            fixedTraits: ["spotted_eggs", "palm_pattern", "white_base"],
            inferredTraits: ["meleagris_gallopavo_derived"],
            unknownTraits: ["pattern_edge_precision"],
            confidenceLevel: "high"
        }
    },
    {
        id: "white_holland",
        name: "White Holland",
        origin: "Europe",
        species: "Turkey",
        eggColor: "Spotted",
        acceptedColors: ["White"],
        weightRoosterKg: 15.0,
        weightHenKg: 8.0,
        size: "Large",
        weightClass: "Heavy",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["APA"],
        geneticProfile: {
            knownGenes: ["c"],
            baselineGenotype: {
                TURKEY__C_Locus: "c/c"
            },
            fixedTraits: ["spotted_eggs", "white_plumage"],
            inferredTraits: ["meleagris_gallopavo_derived"],
            unknownTraits: ["growth_modifiers"],
            confidenceLevel: "high"
        }
    },
    {
        id: "ronquieres_turkey",
        name: "Ronquières",
        origin: "Belgium",
        species: "Turkey",
        eggColor: "Spotted",
        acceptedColors: ["Herminated", "Fawn", "White"],
        weightRoosterKg: 10.0,
        weightHenKg: 5.0,
        size: "Medium",
        weightClass: "Medium",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["EE"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                TURKEY__e_Locus: "e/e",
            },
            fixedTraits: ["hermigon_pattern", "small_size", "hardy"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "rode_ardenner",
        name: "Rode Ardenner",
        origin: "Belgium",
        species: "Turkey",
        eggColor: "Spotted",
        acceptedColors: ["Red"],
        weightRoosterKg: 8.5,
        weightHenKg: 4.5,
        size: "Medium",
        weightClass: "Medium",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["EE"],
        geneticProfile: {
            knownGenes: ["r"],
            baselineGenotype: {
                TURKEY__R_Locus: "r/r"
            },
            fixedTraits: ["spotted_eggs", "red_plumage"],
            inferredTraits: ["meleagris_gallopavo_derived"],
            unknownTraits: ["red_intensity_modifiers"],
            confidenceLevel: "high"
        }
    },
    // --- PEAFOWL ---
    {
        id: "indian_blue_peafowl",
        name: "Indian Blue Peafowl",
        origin: "India",
        species: "Peafowl",
        eggColor: "Cream / Buff",
        acceptedColors: ["Blue", "White", "Pied"],
        weightRoosterKg: 5.0,
        weightHenKg: 3.5,
        size: "Large",
        weightClass: "Heavy",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["International Peafowl Association"],
        geneticProfile: {
            knownGenes: ["IB"],
            baselineGenotype: {
                PEAFOWL__IB_Locus: "IB/IB",
            },
            fixedTraits: ["blue_neck", "barred_wings", "train_feathers"],
            confidenceLevel: "high"
        }
    },
    // --- PHEASANT ---
    {
        id: "ring_necked_pheasant",
        name: "Ring-necked Pheasant (Common)",
        origin: "Asia",
        species: "Pheasant",
        eggColor: "Olive / Brown",
        acceptedColors: ["Wild Type", "Melanistic", "White"],
        weightRoosterKg: 1.2,
        weightHenKg: 0.9,
        size: "Medium",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Game Bird Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                PHEASANT__Wild: "wt/wt",
            },
            fixedTraits: ["white_neck_ring", "long_tail", "game_bird"],
            confidenceLevel: "high"
        }
    },
    {
        id: "mongolian_pheasant",
        name: "Mongolian Pheasant",
        origin: "Mongolia / China",
        species: "Pheasant",
        eggColor: "Olive / Brown",
        acceptedColors: ["Wild Type"],
        weightRoosterKg: 1.3,
        weightHenKg: 1.0,
        size: "Medium",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Game Bird Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                PHEASANT__M_Locus: "M/M",
            },
            fixedTraits: ["darker_plumage", "broad_white_ring", "large_size"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "tenebrosus_pheasant",
        name: "Tenebrosus (Melanistic Mutant)",
        origin: "Captive Bred",
        species: "Pheasant",
        eggColor: "Olive / Brown",
        acceptedColors: ["Black with Green Sheen"],
        weightRoosterKg: 1.2,
        weightHenKg: 0.9,
        size: "Medium",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Game Bird Associations"],
        geneticProfile: {
            knownGenes: ["Mel"],
            baselineGenotype: {
                PHEASANT__Mel_Locus: "Mel/Mel"
            },
            fixedTraits: ["olive_eggs", "melanistic_plumage", "green_iridescence"],
            inferredTraits: ["ring_necked_base"],
            unknownTraits: ["exact_melanin_pathway"],
            confidenceLevel: "high"
        }
    },
    {
        id: "reeves_pheasant",
        name: "Reeves's Pheasant",
        origin: "China",
        species: "Pheasant",
        eggColor: "Olive / Brown",
        acceptedColors: ["Wild Type"],
        weightRoosterKg: 1.5,
        weightHenKg: 1.0,
        size: "Medium",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Game Bird Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                SPECIES_Specific: "Syrmaticus_reevesii",
            },
            fixedTraits: ["golden_plumage", "extremely_long_tail", "aggressive"],
            confidenceLevel: "high"
        }
    },
    {
        id: "golden_pheasant",
        name: "Golden Pheasant",
        origin: "China",
        species: "Pheasant",
        eggColor: "Cream / Buff",
        acceptedColors: ["Wild Type", "Yellow Golden", "Cinnamon"],
        weightRoosterKg: 0.7,
        weightHenKg: 0.6,
        size: "Small",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Ornamental Pheasant Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                SPECIES_Specific: "Chrysolophus_pictus",
            },
            fixedTraits: ["golden_crest", "red_breast", "ruff"],
            confidenceLevel: "high"
        },
        category: "ornamental",
        ornamentalPurpose: ["exhibition", "pet"]
    },
    {
        id: "lady_amherst_pheasant",
        name: "Lady Amherst's Pheasant",
        origin: "Myanmar / China",
        species: "Pheasant",
        eggColor: "Cream / Buff",
        acceptedColors: ["Wild Type"],
        weightRoosterKg: 0.7,
        weightHenKg: 0.6,
        size: "Small",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Ornamental Pheasant Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                SPECIES_Specific: "Chrysolophus_amherstiae",
            },
            fixedTraits: ["white_black_crest", "green_breast", "long_tail"],
            confidenceLevel: "high"
        },
        category: "ornamental",
        ornamentalPurpose: ["exhibition", "pet"]
    },
    {
        id: "silver_pheasant",
        name: "Silver Pheasant",
        origin: "Southeast Asia",
        species: "Pheasant",
        eggColor: "Cream / Buff",
        acceptedColors: ["Wild Type", "Yellow"],
        weightRoosterKg: 1.2,
        weightHenKg: 1.0,
        size: "Medium",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Ornamental Pheasant Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                SPECIES_Specific: "Lophura_nycthemera",
            },
            fixedTraits: ["silver_white_upper", "black_under", "red_face_wattles"],
            confidenceLevel: "high"
        },
        category: "ornamental",
        ornamentalPurpose: ["exhibition", "pet"]
    },
    // --- QUAIL ---
    {
        id: "coturnix_japanese",
        name: "Coturnix (Japanese Quail)",
        origin: "Japan / East Asia",
        species: "Quail",
        eggColor: "Speckled Brown / Cream",
        acceptedColors: ["Wild Type", "Fawn", "White", "Tuxedo", "Silver"],
        weightRoosterKg: 0.14,
        weightHenKg: 0.16,
        size: "Small",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Quail Breeders Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                QUAIL__Wild: "wt/wt",
            },
            fixedTraits: ["brown_speckled", "fast_maturing", "high_production"],
            confidenceLevel: "high"
        }
    },
    {
        id: "jumbo_coturnix",
        name: "Jumbo Coturnix",
        origin: "Selective Breeding (USA)",
        species: "Quail",
        eggColor: "Speckled Brown / Cream",
        acceptedColors: ["Wild Type", "White", "Golden"],
        weightRoosterKg: 0.30,
        weightHenKg: 0.35,
        size: "Small",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Quail Breeders Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                QUAIL__Wild: "wt/wt",
            },
            fixedTraits: ["large_size", "brown_speckled", "meat_bird"],
            confidenceLevel: "high"
        }
    },
    {
        id: "pharaoh_coturnix",
        name: "Pharaoh Coturnix",
        origin: "Selective Breeding",
        species: "Quail",
        eggColor: "Speckled Brown / Cream",
        acceptedColors: ["Brown / Wild Type"],
        weightRoosterKg: 0.18,
        weightHenKg: 0.20,
        size: "Small",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Quail Breeders Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                QUAIL__Wild: "wt/wt",
            },
            fixedTraits: ["wild_type_plumage", "standard_size", "good_layer"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "tibetan_coturnix",
        name: "Tibetan Coturnix",
        origin: "Selective Breeding",
        species: "Quail",
        eggColor: "Speckled Brown / Cream",
        acceptedColors: ["Buff / Cream"],
        weightRoosterKg: 0.14,
        weightHenKg: 0.16,
        size: "Small",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Quail Breeders Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                QUAIL__E_Locus: "E/E",
            },
            fixedTraits: ["dark_brown_plumage", "white_breast_patch_variable"],
            confidenceLevel: "medium"
        }
    },
    {
        id: "bobwhite_quail",
        name: "Bobwhite Quail",
        origin: "North America",
        species: "Quail",
        eggColor: "White / Cream",
        acceptedColors: ["Wild Type", "Tennessee Red", "Mexican Speckled"],
        weightRoosterKg: 0.18,
        weightHenKg: 0.17,
        size: "Small",
        weightClass: "Light",
        official: true,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: ["Game Bird Associations"],
        geneticProfile: {
            knownGenes: [],
            baselineGenotype: {
                SPECIES_Specific: "Colinus_virginianus",
            },
            fixedTraits: ["white_throat_male", "brown_body", "game_bird"],
            confidenceLevel: "high"
        }
    }
];

/**
 * Validates the breed data against architectural rules and catalog coverage.
 */
export function validateBreedStandardsData(): void {
    console.log("🔍 Validating Breed Standards Data against Manifest...");

    const manifestPath = path.join(__dirname, 'generated', 'genetic_loci_manifest.json');
    if (!fs.existsSync(manifestPath)) {
        throw new Error(`Manifest not found at ${manifestPath}. Run logical manifest generator first.`);
    }

    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf-8'));
    const validLoci = new Map<string, { species: string, alleles: Set<string> }>();

    for (const locus of manifest.loci) {
        validLoci.set(locus.locusId, {
            species: locus.species,
            alleles: new Set(locus.alleles)
        });
    }

    const errors: string[] = [];

    for (const breed of breedStandardsData) {
        const breedSpeciesUpper = breed.species.toUpperCase();
        const baseline = breed.geneticProfile.baselineGenotype || {};

        for (const [locusId, genotype] of Object.entries(baseline)) {
            const locusDef = validLoci.get(locusId);

            if (!locusDef) {
                // SPECIES_Specific is allowed if it exists in manifest, but checking for it explicitly is safer
                if (locusId === "SPECIES_Specific") continue;

                errors.push(`[${breed.id}] Unknown locus ID: ${locusId} (not in manifest)`);
                continue;
            }

            // Strict Species Check
            if (locusDef.species !== breedSpeciesUpper) {
                // If the species don't match, flag it
                errors.push(`[${breed.id}] Locus ${locusId} belongs to ${locusDef.species}, but breed is ${breed.species}`);
            }

            // Allele Check
            const alleles = genotype.split('/');
            for (const allele of alleles) {
                if (!locusDef.alleles.has(allele)) {
                    errors.push(`[${breed.id}] Invalid allele '${allele}' for ${locusId}. Valid: ${Array.from(locusDef.alleles).join(', ')}`);
                }
            }
        }
    }

    if (errors.length > 0) {
        console.error("❌ VALIDATION FAILED:");
        errors.forEach(err => console.error(`  - ${err}`));
        throw new Error("Breed Standards Validation Failed.");
    }
    console.log("✅ Breed Standards Data Validated (Architecture + Catalog Coverage).");
}

export async function seedBreedStandards(db: admin.firestore.Firestore, isDryRun: boolean, verbose: boolean = false): Promise<void> {
    // Run validation first!
    validateBreedStandardsDataV2();

    if (verbose) console.log("🐔 Starting Breed Standards seed...");
    const collectionRef = db.collection('breedStandards');

    // Process in chunks to avoid batch limits if list grows
    const BATCH_SIZE = 400; // Firestore batch limit is 500
    let count = 0;

    for (let i = 0; i < breedStandardsData.length; i += BATCH_SIZE) {
        const chunk = breedStandardsData.slice(i, i + BATCH_SIZE);
        const currentBatch = db.batch();

        for (const breed of chunk) {
            const docRef = collectionRef.doc(breed.id);
            if (!isDryRun) {
                currentBatch.set(docRef, {
                    ...breed,
                    lastUpdated: admin.firestore.FieldValue.serverTimestamp()
                }, { merge: true });
            }
            if (verbose) console.log(`${isDryRun ? '[DRY RUN] ' : ''}Upserting breed: ${breed.id}`);
            count++;
        }

        if (!isDryRun) {
            await currentBatch.commit();
        }
    }

    if (verbose) {
        if (isDryRun) console.log(`ℹ️ [DRY RUN] Would have upserted ${count} breed standards.`);
        else console.log(`✅ upserted ${count} breed standards.`);
    }
}

export function validateBreedStandardsDataV2(): void {
    console.log("🔍 Validating Breed Standards Data against Manifest (V2)...");

    const manifestPath = path.join(__dirname, 'generated', 'genetic_loci_manifest.json');
    if (!fs.existsSync(manifestPath)) {
        throw new Error(`Manifest not found at ${manifestPath}. Run logical manifest generator first.`);
    }

    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf-8'));
    const validLoci = new Map<string, { species: string, alleles: Set<string> }>();

    for (const locus of manifest.loci) {
        validLoci.set(locus.locusId, {
            species: locus.species,
            alleles: new Set(locus.alleles)
        });
    }

    const errors: string[] = [];

    for (const breed of breedStandardsData) {
        // Handle potential case differences if manifest uses uppercase species
        const breedSpeciesUpper = breed.species.toUpperCase();
        const baseline = breed.geneticProfile.baselineGenotype || {};

        for (const [locusId, genotype] of Object.entries(baseline)) {
            // 1. Check Locus Existence
            const locusDef = validLoci.get(locusId);
            if (!locusDef) {
                if (locusId === "SPECIES_Specific") continue;
                errors.push(`[${breed.id}] Unknown locus ID: ${locusId} (not in manifest)`);
                continue;
            }

            // 2. Check Species Matching
            if (locusDef.species !== "ALL" && locusDef.species !== breedSpeciesUpper) {
                if (breedSpeciesUpper === "CHICKEN" && locusDef.species !== "CHICKEN") {
                    errors.push(`[${breed.id}] Chicken using non-chicken locus: ${locusId}`);
                } else if (breedSpeciesUpper !== "CHICKEN" && locusDef.species === "CHICKEN") {
                    errors.push(`[${breed.id}] ${breed.species} using Chicken locus: ${locusId}`);
                }
            }

            // 3. Check Alleles
            if (locusDef.alleles.has("Any")) continue;

            const alleles = genotype.split('/');
            for (const allele of alleles) {
                if (!locusDef.alleles.has(allele)) {
                    errors.push(`[${breed.id}] Invalid allele '${allele}' for ${locusId}. Valid: ${Array.from(locusDef.alleles).join(', ')}`);
                }
            }
        }
    }

    if (errors.length > 0) {
        console.error("❌ VALIDATION FAILED:");
        errors.forEach(err => console.error(`  - ${err}`));
        throw new Error("Breed Standards Validation Failed.");
    }
    console.log("✅ Breed Standards Data Validated (Architecture + Catalog Coverage).");
}


