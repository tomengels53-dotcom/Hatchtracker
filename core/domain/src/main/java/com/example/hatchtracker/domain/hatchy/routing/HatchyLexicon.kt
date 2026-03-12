package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.KnowledgeTopic

/**
 * Static lexicon content for Hatchy.
 * Separate from normalization logic to keep definitions clean.
 */
object HatchyLexicon {

    val BreedAliases = mapOf(
        "rir" to "Rhode Island Red",
        "jg" to "Jersey Giant",
        "ls" to "Light Sussex",
        "wlh" to "White Leghorn",
        "br" to "Barred Rock",
        "bjg" to "Black Jersey Giant",
        "wjg" to "White Jersey Giant"
    )

    val SpeciesAliases = mapOf(
        "chicken" to PoultrySpecies.CHICKEN,
        "chickens" to PoultrySpecies.CHICKEN,
        "hen" to PoultrySpecies.CHICKEN,
        "hens" to PoultrySpecies.CHICKEN,
        "rooster" to PoultrySpecies.CHICKEN,
        "roosters" to PoultrySpecies.CHICKEN,
        "cockerel" to PoultrySpecies.CHICKEN,
        "cockerels" to PoultrySpecies.CHICKEN,
        "pullet" to PoultrySpecies.CHICKEN,
        "pullets" to PoultrySpecies.CHICKEN,
        "duck" to PoultrySpecies.DUCK,
        "ducks" to PoultrySpecies.DUCK,
        "duckling" to PoultrySpecies.DUCK,
        "ducklings" to PoultrySpecies.DUCK,
        "drake" to PoultrySpecies.DUCK,
        "drakes" to PoultrySpecies.DUCK,
        "goose" to PoultrySpecies.GOOSE,
        "geese" to PoultrySpecies.GOOSE,
        "gosling" to PoultrySpecies.GOOSE,
        "goslings" to PoultrySpecies.GOOSE,
        "gander" to PoultrySpecies.GOOSE,
        "ganders" to PoultrySpecies.GOOSE,
        "turkey" to PoultrySpecies.TURKEY,
        "turkeys" to PoultrySpecies.TURKEY,
        "quail" to PoultrySpecies.QUAIL,
        "quails" to PoultrySpecies.QUAIL,
        "guinea" to PoultrySpecies.GUINEA_FOWL,
        "guineas" to PoultrySpecies.GUINEA_FOWL,
        "peafowl" to PoultrySpecies.PEAFOWL,
        "peafowls" to PoultrySpecies.PEAFOWL,
        "peacock" to PoultrySpecies.PEAFOWL,
        "peacocks" to PoultrySpecies.PEAFOWL
    )

    val IncubationTopics = mapOf(
        "how long" to IncubationTopic.Duration,
        "days" to IncubationTopic.Duration,
        "period" to IncubationTopic.Duration,
        "humidity" to IncubationTopic.Humidity,
        "moisture" to IncubationTopic.Humidity,
        "wet" to IncubationTopic.Humidity,
        "dry" to IncubationTopic.Humidity,
        "temperature" to IncubationTopic.Temperature,
        "temp" to IncubationTopic.Temperature,
        "heat" to IncubationTopic.Temperature,
        "degrees" to IncubationTopic.Temperature,
        "candl" to IncubationTopic.Candling,
        "light" to IncubationTopic.Candling,
        "turn" to IncubationTopic.Turning,
        "rotat" to IncubationTopic.Turning,
        "lockdown" to IncubationTopic.Lockdown,
        "day 18" to IncubationTopic.Lockdown,
        "day 19" to IncubationTopic.Lockdown,
        "stop turning" to IncubationTopic.Lockdown,
        "hatch day" to IncubationTopic.HatchDay,
        "pip" to IncubationTopic.HatchDay,
        "zipp" to IncubationTopic.HatchDay,
        "health" to IncubationTopic.Health,
        "sick" to IncubationTopic.Health,
        "bubbles" to IncubationTopic.Health
    )

    val BreedingTopics = mapOf(
        "line breed" to BreedingTopic.LineBreeding,
        "cross" to BreedingTopic.CrossBreeding,
        "mix" to BreedingTopic.CrossBreeding,
        "select" to BreedingTopic.Selection,
        "cull" to BreedingTopic.Selection,
        "genetic" to BreedingTopic.Genetics,
        "dna" to BreedingTopic.Genetics,
        "trait" to BreedingTopic.Genetics,
        "stabili" to BreedingTopic.Stability,
        "advice" to BreedingTopic.Advice,
        "recommend" to BreedingTopic.Advice,
        "suggest" to BreedingTopic.Advice,
        "should i" to BreedingTopic.Advice,
        "simulat" to BreedingTopic.Simulation,
        "predict" to BreedingTopic.Simulation,
        "fore" to BreedingTopic.Simulation,
        "what if" to BreedingTopic.Outcome,
        "happen" to BreedingTopic.Outcome,
        "result" to BreedingTopic.Outcome
    )

    val NurseryStatusTopics = mapOf(
        "how many" to NurseryStatusTopic.ActiveCount,
        "count" to NurseryStatusTopic.ActiveCount,
        "total" to NurseryStatusTopic.ActiveCount,
        "ready" to NurseryStatusTopic.ReadyToMove,
        "move" to NurseryStatusTopic.ReadyToMove,
        "transition" to NurseryStatusTopic.ReadyToMove,
        "status" to NurseryStatusTopic.BrooderStatus,
        "brooder" to NurseryStatusTopic.BrooderStatus,
        "dead" to NurseryStatusTopic.Losses,
        "died" to NurseryStatusTopic.Losses,
        "loss" to NurseryStatusTopic.Losses,
        "age" to NurseryStatusTopic.AgeGroups,
        "week" to NurseryStatusTopic.AgeGroups,
        "old" to NurseryStatusTopic.AgeGroups
    )

    val NurseryGuidanceTopics = mapOf(
        "temp" to NurseryGuidanceTopic.Temperature,
        "heat" to NurseryGuidanceTopic.Temperature,
        "feed" to NurseryGuidanceTopic.Feeding,
        "eat" to NurseryGuidanceTopic.Feeding,
        "coop" to NurseryGuidanceTopic.CoopTransition
    )

    val FinanceSummaryTopics = mapOf(
        "total" to FinanceSummaryTopic.TotalSpend,
        "spend" to FinanceSummaryTopic.TotalSpend,
        "cost" to FinanceSummaryTopic.TotalSpend,
        "breakdown" to FinanceSummaryTopic.CategoryBreakdown,
        "expense" to FinanceSummaryTopic.RecentExpenses,
        "recent" to FinanceSummaryTopic.RecentExpenses,
        "trend" to FinanceSummaryTopic.MonthlyTrend,
        "chart" to FinanceSummaryTopic.MonthlyTrend
    )

    val FinanceHelpTopics = mapOf(
        "log" to FinanceHelpTopic.LogExpense,
        "add" to FinanceHelpTopic.LogExpense,
        "edit" to FinanceHelpTopic.EditEntry,
        "help" to FinanceHelpTopic.Other,
        "how" to FinanceHelpTopic.Other
    )

    val EquipmentStatusTopics = mapOf(
        "active" to EquipmentStatusTopic.ActiveDevices,
        "online" to EquipmentStatusTopic.ActiveDevices,
        "sensor" to EquipmentStatusTopic.SensorStatus,
        "read" to EquipmentStatusTopic.SensorStatus,
        "capacity" to EquipmentStatusTopic.Capacity,
        "full" to EquipmentStatusTopic.Capacity,
        "maintenance" to EquipmentStatusTopic.Maintenance,
        "fix" to EquipmentStatusTopic.Maintenance,
        "alert" to EquipmentStatusTopic.Alerts
    )

    val EquipmentHelpTopics = mapOf(
        "add" to EquipmentHelpTopic.AddDevice,
        "new" to EquipmentHelpTopic.AddDevice,
        "help" to EquipmentHelpTopic.Other,
        "how" to EquipmentHelpTopic.Other
    )

    val PoultryTopics = mapOf(
        "care" to PoultryTopic.Care,
        "raising" to PoultryTopic.Care,
        "sick" to PoultryTopic.Health,
        "ill" to PoultryTopic.Health,
        "disease" to PoultryTopic.Health,
        "parasite" to PoultryTopic.Health,
        "behavior" to PoultryTopic.Behavior,
        "aggression" to PoultryTopic.Behavior,
        "pecking" to PoultryTopic.Behavior,
        "coop" to PoultryTopic.Housing,
        "house" to PoultryTopic.Housing,
        "shed" to PoultryTopic.Housing,
        "feed" to PoultryTopic.Feeding,
        "eat" to PoultryTopic.Feeding,
        "diet" to PoultryTopic.Feeding,
        "nutrition" to PoultryTopic.Feeding
    )

    val TraitTopics = mapOf(
        "egg color" to KnowledgeTopic.EGG_TRAITS,
        "egg size" to KnowledgeTopic.EGG_TRAITS,
        "laying" to KnowledgeTopic.EGG_TRAITS,
        "production" to KnowledgeTopic.EGG_TRAITS,
        "annual eggs" to KnowledgeTopic.EGG_TRAITS,
        "productive" to KnowledgeTopic.EGG_TRAITS,
        "docile" to KnowledgeTopic.TEMPERAMENT,
        "friendly" to KnowledgeTopic.TEMPERAMENT,
        "aggressive" to KnowledgeTopic.TEMPERAMENT,
        "mean" to KnowledgeTopic.TEMPERAMENT,
        "tame" to KnowledgeTopic.TEMPERAMENT,
        "calm" to KnowledgeTopic.TEMPERAMENT,
        "active" to KnowledgeTopic.TEMPERAMENT,
        "noise" to KnowledgeTopic.TEMPERAMENT,
        "loud" to KnowledgeTopic.TEMPERAMENT,
        "cold hardy" to KnowledgeTopic.HARDINESS,
        "heat tolerant" to KnowledgeTopic.HARDINESS,
        "winter" to KnowledgeTopic.HARDINESS,
        "summer" to KnowledgeTopic.HARDINESS,
        "tough" to KnowledgeTopic.HARDINESS,
        "robust" to KnowledgeTopic.HARDINESS,
        "meat" to KnowledgeTopic.UTILITY_PURPOSE,
        "purpose" to KnowledgeTopic.UTILITY_PURPOSE,
        "dual purpose" to KnowledgeTopic.UTILITY_PURPOSE,
        "broiler" to KnowledgeTopic.UTILITY_PURPOSE,
        "growth rate" to KnowledgeTopic.UTILITY_PURPOSE,
        "ornamental" to KnowledgeTopic.UTILITY_PURPOSE,
        "comb" to KnowledgeTopic.PHYSICAL_TRAITS,
        "feather" to KnowledgeTopic.PHYSICAL_TRAITS,
        "color" to KnowledgeTopic.PHYSICAL_TRAITS,
        "legs" to KnowledgeTopic.PHYSICAL_TRAITS,
        "size" to KnowledgeTopic.PHYSICAL_TRAITS,
        "weight" to KnowledgeTopic.PHYSICAL_TRAITS,
        "beard" to KnowledgeTopic.PHYSICAL_TRAITS,
        "crest" to KnowledgeTopic.PHYSICAL_TRAITS,
        "disease" to KnowledgeTopic.HEALTH_ROBUSTNESS,
        "lifespan" to KnowledgeTopic.HEALTH_ROBUSTNESS,
        "resistance" to KnowledgeTopic.HEALTH_ROBUSTNESS,
        "healthy" to KnowledgeTopic.HEALTH_ROBUSTNESS
    )
}
