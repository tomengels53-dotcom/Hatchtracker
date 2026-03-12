package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.BreedStandard
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts structured entities from natural language queries.
 * Supports breeds, species, traits, and domain-specific keywords.
 */
@Singleton
class HatchyEntityExtractor @Inject constructor(
    private val breedRepository: IBreedStandardRepository
) {
    private val traitKeywords = mapOf(
        "egg" to "egg_production",
        "lay" to "egg_production",
        "meat" to "meat_quality",
        "size" to "body_size",
        "weight" to "body_size",
        "heavy" to "body_size",
        "temperament" to "temperament",
        "calm" to "temperament",
        "docile" to "temperament",
        "friendly" to "temperament",
        "broody" to "broodiness",
        "sit" to "broodiness",
        "plumage" to "plumage",
        "color" to "color",
        "growth" to "growth_rate"
    )

    private val moduleKeywords = mapOf(
        "flock" to "FLOCK",
        "incubation" to "INCUBATION",
        "hatch" to "INCUBATION",
        "nursery" to "NURSERY",
        "brooder" to "NURSERY",
        "finance" to "FINANCE",
        "money" to "FINANCE",
        "cost" to "FINANCE",
        "spend" to "FINANCE",
        "equipment" to "EQUIPMENT",
        "device" to "EQUIPMENT",
        "sensor" to "EQUIPMENT",
        "breeding" to "BREEDING"
    )

    private val abbreviationMap = mapOf(
        "rir" to "rir", // Rhode Island Red
        "jg" to "jg",   // Jersey Giant
        "ls" to "ls",   // Light Sussex
        "wlh" to "wlh"  // White Leghorn
    )

    fun extract(query: String): List<HatchyEntity> {
        val q = query.lowercase().replace(" x ", " ").replace(" cross ", " ")
        val entities = mutableListOf<HatchyEntity>()

        // 1. Breed Extraction (with Alises)
        val breeds = breedRepository.getAllBreeds()
        
        // Match Abbreviations/Aliases first
        HatchyLexicon.BreedAliases.forEach { (abbr, fullName) ->
            if (hasWordBoundaryMatch(q, abbr)) {
                val breed = breeds.find { it.name.equals(fullName, ignoreCase = true) }
                if (breed != null) {
                    entities.add(HatchyEntity(
                        type = EntityType.BREED,
                        value = breed.id,
                        originalText = abbr,
                        metadata = mapOf("species" to breed.species)
                    ))
                }
            }
        }

        // Match Full Names
        breeds.forEach { breed ->
            val fullName = breed.name.lowercase()
            val compactName = fullName.replace(" ", "")
            if (q.contains(fullName) || q.contains(compactName)) {
                if (entities.none { it.value == breed.id }) {
                    entities.add(HatchyEntity(
                        type = EntityType.BREED,
                        value = breed.id,
                        originalText = breed.name,
                        metadata = mapOf("species" to breed.species)
                    ))
                }
            }
        }

        // 2. Species Extraction
        HatchyLexicon.SpeciesAliases.forEach { (alias, species) ->
            if (q.contains(alias)) {
                if (entities.none { it.type == EntityType.POULTRY_SPECIES && it.value == species.name }) {
                    entities.add(HatchyEntity(
                        type = EntityType.POULTRY_SPECIES,
                        value = species.name,
                        originalText = alias
                    ))
                }
            }
        }

        HatchyLexicon.IncubationTopics.forEach { (keyword, topic) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(
                    type = EntityType.INCUBATION_TOPIC,
                    value = topic.toString(),
                    originalText = keyword
                ))
            }
        }

        HatchyLexicon.BreedingTopics.forEach { (keyword, topic) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(
                    type = EntityType.BREEDING_TOPIC,
                    value = topic.toString(),
                    originalText = keyword
                ))
            }
        }

        HatchyLexicon.NurseryStatusTopics.forEach { (keyword, topic) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(
                    type = EntityType.NURSERY_STATUS_TOPIC,
                    value = topic.toString(),
                    originalText = keyword
                ))
            }
        }

        HatchyLexicon.NurseryGuidanceTopics.forEach { (keyword, topic) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(
                    type = EntityType.NURSERY_GUIDANCE_TOPIC,
                    value = topic.toString(),
                    originalText = keyword
                ))
            }
        }

        HatchyLexicon.FinanceSummaryTopics.forEach { (keyword, topic) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(
                    type = EntityType.FINANCE_SUMMARY_TOPIC,
                    value = topic.toString(),
                    originalText = keyword
                ))
            }
        }

        HatchyLexicon.FinanceHelpTopics.forEach { (keyword, topic) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(
                    type = EntityType.FINANCE_HELP_TOPIC,
                    value = topic.toString(),
                    originalText = keyword
                ))
            }
        }

        HatchyLexicon.EquipmentStatusTopics.forEach { (keyword, topic) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(
                    type = EntityType.EQUIPMENT_STATUS_TOPIC,
                    value = topic.toString(),
                    originalText = keyword
                ))
            }
        }

        HatchyLexicon.EquipmentHelpTopics.forEach { (keyword, topic) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(
                    type = EntityType.EQUIPMENT_HELP_TOPIC,
                    value = topic.toString(),
                    originalText = keyword
                ))
            }
        }

        HatchyLexicon.PoultryTopics.forEach { (keyword, topic) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(
                    type = EntityType.POULTRY_TOPIC,
                    value = topic.toString(),
                    originalText = keyword
                ))
            }
        }

        HatchyLexicon.TraitTopics.forEach { (keyword, topic) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(
                    type = EntityType.TRAIT,
                    value = topic.toString(),
                    originalText = keyword
                ))
            }
        }

        // 4. Traits & Modules (Legacy/Direct)
        traitKeywords.forEach { (keyword, traitId) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(EntityType.TRAIT, traitId, keyword))
            }
        }

        moduleKeywords.forEach { (keyword, module) ->
            if (q.contains(keyword)) {
                entities.add(HatchyEntity(EntityType.MODULE, module, keyword))
            }
        }

        return entities.distinctBy { it.type.toString() + it.value }
    }

    private fun hasWordBoundaryMatch(query: String, keyword: String): Boolean {
        val pattern = "\\b${Regex.escape(keyword)}\\b".toRegex()
        return pattern.containsMatchIn(query)
    }
}
