package com.example.hatchtracker.domain.hatchy.routing

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Maps intents to the correct resolver implementation.
 * Ensures domain modularity and easy expansion for new modules.
 */
@Singleton
class HatchyResolverRegistry @Inject constructor(
    private val appNavResolver: Provider<AppNavigationResolver>,
    private val breedKnowledgeResolver: Provider<BreedKnowledgeResolver>,
    private val breedingSimulationResolver: Provider<BreedingSimulationResolver>,
    private val breedingGuidanceResolver: Provider<BreedingGuidanceResolver>,
    private val poultryKnowledgeResolver: Provider<PoultryKnowledgeResolver>,
    private val incubationGuidanceResolver: Provider<IncubationGuidanceResolver>,
    private val incubationStatusResolver: Provider<IncubationStatusResolver>,
    private val nurseryGuidanceResolver: Provider<NurseryGuidanceResolver>,
    private val nurseryStatusResolver: Provider<NurseryStatusResolver>,
    private val financeHelpResolver: Provider<FinanceHelpResolver>,
    private val financeSummaryResolver: Provider<FinanceSummaryResolver>,
    private val equipmentHelpResolver: Provider<EquipmentHelpResolver>,
    private val equipmentStatusResolver: Provider<EquipmentStatusResolver>,
    private val flockBreedingAdvisorResolver: Provider<FlockBreedingAdvisorResolver>,
    private val userDataAwareResolver: Provider<UserDataAwareResolver>,
    private val fallbackResolver: Provider<FallbackResolver>
) {

    fun getAllResolvers(): List<HatchyResolver> {
        return listOf(
            appNavResolver.get(),
            breedKnowledgeResolver.get(),
            breedingSimulationResolver.get(),
            breedingGuidanceResolver.get(),
            poultryKnowledgeResolver.get(),
            incubationGuidanceResolver.get(),
            incubationStatusResolver.get(),
            nurseryGuidanceResolver.get(),
            nurseryStatusResolver.get(),
            financeHelpResolver.get(),
            financeSummaryResolver.get(),
            equipmentHelpResolver.get(),
            equipmentStatusResolver.get(),
            flockBreedingAdvisorResolver.get(),
            userDataAwareResolver.get(),
            fallbackResolver.get()
        )
    }

    fun getResolver(intent: HatchyIntent): HatchyResolver {
        return when (intent) {
            HatchyIntent.APP_NAVIGATION -> appNavResolver.get()
            HatchyIntent.BREED_INFO, HatchyIntent.BREED_COMPARISON -> breedKnowledgeResolver.get()
            HatchyIntent.CROSSBREED_OUTCOME -> breedingSimulationResolver.get()
            HatchyIntent.BREEDING_GUIDANCE -> breedingGuidanceResolver.get()
            HatchyIntent.USER_FLOCK_RECOMMENDATION -> flockBreedingAdvisorResolver.get()
            HatchyIntent.INCUBATION_GUIDANCE -> incubationGuidanceResolver.get()
            HatchyIntent.INCUBATION_STATUS -> incubationStatusResolver.get()
            HatchyIntent.NURSERY_GUIDANCE -> nurseryGuidanceResolver.get()
            HatchyIntent.NURSERY_STATUS -> nurseryStatusResolver.get()
            HatchyIntent.FINANCE_HELP -> financeHelpResolver.get()
            HatchyIntent.FINANCE_SUMMARY -> financeSummaryResolver.get()
            HatchyIntent.EQUIPMENT_HELP -> equipmentHelpResolver.get()
            HatchyIntent.EQUIPMENT_STATUS -> equipmentStatusResolver.get()
            HatchyIntent.GENERAL_POULTRY, HatchyIntent.POULTRY_HEALTH -> poultryKnowledgeResolver.get()
            HatchyIntent.USER_DATA_QUERY -> userDataAwareResolver.get()
            else -> fallbackResolver.get()
        }
    }
}

