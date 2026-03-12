package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*

/**
 * Maps natural language phrases to normalized [TraitConstraint] and [KnowledgeTopic].
 * Supports species-neutral and species-specific traits.
 */
object TraitTopicLexicon {

    /**
     * Phrases mapped to their primary KnowledgeTopic and specific TraitConstraint.
     */
    val traitMappings: Map<String, Pair<KnowledgeTopic, TraitConstraint>> = mapOf(
        // Egg Color
        "blue egg" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.BLUE)),
        "blue eggs" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.BLUE)),
        "green egg" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.GREEN)),
        "green eggs" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.GREEN)),
        "brown egg" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.BROWN)),
        "brown eggs" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.BROWN)),
        "white egg" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.WHITE)),
        "white eggs" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.WHITE)),
        "cream egg" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.CREAM)),
        "cream eggs" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.CREAM)),
        "dark brown egg" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.DARK_BROWN)),
        "dark brown eggs" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.DARK_BROWN)),
        "speckled egg" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.SPECKLED)),
        "speckled eggs" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.SPECKLED)),
        "olive egg" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.OLIVE)),
        "olive eggs" to (KnowledgeTopic.EGG_COLOR to TraitConstraint(TraitDimension.EGG_COLOR, EggColor.OLIVE)),

        // Temperament
        "docile" to (KnowledgeTopic.TEMPERAMENT_DOCILITY to TraitConstraint(TraitDimension.TEMPERAMENT_DOCILITY, TemperamentLevel.DOCILE)),
        "friendly" to (KnowledgeTopic.TEMPERAMENT_DOCILITY to TraitConstraint(TraitDimension.TEMPERAMENT_DOCILITY, TemperamentLevel.DOCILE)),
        "calm" to (KnowledgeTopic.TEMPERAMENT_DOCILITY to TraitConstraint(TraitDimension.TEMPERAMENT_DOCILITY, TemperamentLevel.CALM)),
        "tame" to (KnowledgeTopic.TEMPERAMENT_DOCILITY to TraitConstraint(TraitDimension.TEMPERAMENT_DOCILITY, TemperamentLevel.CALM)),
        "active" to (KnowledgeTopic.TEMPERAMENT_DOCILITY to TraitConstraint(TraitDimension.TEMPERAMENT_DOCILITY, TemperamentLevel.ACTIVE)),
        "aggressive" to (KnowledgeTopic.TEMPERAMENT_DOCILITY to TraitConstraint(TraitDimension.TEMPERAMENT_DOCILITY, TemperamentLevel.AGGRESSIVE)),
        "mean" to (KnowledgeTopic.TEMPERAMENT_DOCILITY to TraitConstraint(TraitDimension.TEMPERAMENT_DOCILITY, TemperamentLevel.AGGRESSIVE)),
        "skittish" to (KnowledgeTopic.TEMPERAMENT_DOCILITY to TraitConstraint(TraitDimension.TEMPERAMENT_DOCILITY, TemperamentLevel.SKITTISH)),

        // Hardiness
        "cold hardy" to (KnowledgeTopic.COLD_HARDINESS to TraitConstraint(TraitDimension.COLD_HARDINESS, HardinessLevel.COLD_HARDY)),
        "winter hardy" to (KnowledgeTopic.COLD_HARDINESS to TraitConstraint(TraitDimension.COLD_HARDINESS, HardinessLevel.COLD_HARDY)),
        "heat tolerant" to (KnowledgeTopic.HEAT_TOLERANCE to TraitConstraint(TraitDimension.HEAT_TOLERANCE, HardinessLevel.HEAT_TOLERANT)),
        "summer hardy" to (KnowledgeTopic.HEAT_TOLERANCE to TraitConstraint(TraitDimension.HEAT_TOLERANCE, HardinessLevel.HEAT_TOLERANT)),
        "robust" to (KnowledgeTopic.HARDINESS to TraitConstraint(TraitDimension.COLD_HARDINESS, HardinessLevel.ROBUST)),
        "tough" to (KnowledgeTopic.HARDINESS to TraitConstraint(TraitDimension.COLD_HARDINESS, HardinessLevel.ROBUST)),

        // Body Size
        "bantam" to (KnowledgeTopic.BODY_SIZE to TraitConstraint(TraitDimension.BODY_SIZE, BodySizeClass.BANTAM)),
        "small" to (KnowledgeTopic.BODY_SIZE to TraitConstraint(TraitDimension.BODY_SIZE, BodySizeClass.BANTAM)),
        "light" to (KnowledgeTopic.BODY_SIZE to TraitConstraint(TraitDimension.BODY_SIZE, BodySizeClass.LIGHT)),
        "medium" to (KnowledgeTopic.BODY_SIZE to TraitConstraint(TraitDimension.BODY_SIZE, BodySizeClass.MEDIUM)),
        "heavy" to (KnowledgeTopic.BODY_SIZE to TraitConstraint(TraitDimension.BODY_SIZE, BodySizeClass.HEAVY)),
        "large" to (KnowledgeTopic.BODY_SIZE to TraitConstraint(TraitDimension.BODY_SIZE, BodySizeClass.HEAVY)),
        "huge" to (KnowledgeTopic.BODY_SIZE to TraitConstraint(TraitDimension.BODY_SIZE, BodySizeClass.EXTRA_HEAVY)),

        // Usage
        "layer" to (KnowledgeTopic.EGG_PRODUCTION_RATE to TraitConstraint(TraitDimension.MEAT_YIELD, PrimaryUsage.LAYER)), // Usage often maps to better production
        "meat" to (KnowledgeTopic.MEAT_YIELD to TraitConstraint(TraitDimension.MEAT_YIELD, PrimaryUsage.MEAT)),
        "dual purpose" to (KnowledgeTopic.UTILITY_PURPOSE to TraitConstraint(TraitDimension.MEAT_YIELD, PrimaryUsage.DUAL_PURPOSE)),
        "ornamental" to (KnowledgeTopic.PHYSICAL_TRAITS to TraitConstraint(TraitDimension.MEAT_YIELD, PrimaryUsage.ORNAMENTAL)),
        "conservation" to (KnowledgeTopic.UTILITY_PURPOSE to TraitConstraint(TraitDimension.MEAT_YIELD, PrimaryUsage.CONSERVATION)),

        // Broodiness
        "broody" to (KnowledgeTopic.BROODINESS_LEVEL to TraitConstraint(TraitDimension.BROODINESS_LEVEL, BroodinessLevel.FREQUENT)),
        "sitter" to (KnowledgeTopic.BROODINESS_LEVEL to TraitConstraint(TraitDimension.BROODINESS_LEVEL, BroodinessLevel.FREQUENT)),
        "not broody" to (KnowledgeTopic.BROODINESS_LEVEL to TraitConstraint(TraitDimension.BROODINESS_LEVEL, BroodinessLevel.NEVER)),
        "seldom broody" to (KnowledgeTopic.BROODINESS_LEVEL to TraitConstraint(TraitDimension.BROODINESS_LEVEL, BroodinessLevel.SELDOM)),

        // Comb Type
        "single comb" to (KnowledgeTopic.COMB_TYPE to TraitConstraint(TraitDimension.COMB_TYPE, CombType.SINGLE)),
        "pea comb" to (KnowledgeTopic.COMB_TYPE to TraitConstraint(TraitDimension.COMB_TYPE, CombType.PEA)),
        "rose comb" to (KnowledgeTopic.COMB_TYPE to TraitConstraint(TraitDimension.COMB_TYPE, CombType.ROSE)),
        "walnut comb" to (KnowledgeTopic.COMB_TYPE to TraitConstraint(TraitDimension.COMB_TYPE, CombType.WALNUT)),
        "v comb" to (KnowledgeTopic.COMB_TYPE to TraitConstraint(TraitDimension.COMB_TYPE, CombType.V_SHAPE)),
        "buttercup comb" to (KnowledgeTopic.COMB_TYPE to TraitConstraint(TraitDimension.COMB_TYPE, CombType.BUTTERCUP))
    )

    /**
     * Partial match synonyms for ranking logic.
     */
    val partialSynonyms: Map<String, String> = mapOf(
        "calm" to "docile",
        "docile" to "calm",
        "tough" to "robust",
        "robust" to "tough",
        "hardy" to "robust",
        "dark brown" to "brown",
        "light" to "small",
        "heavy" to "large"
    )
}
