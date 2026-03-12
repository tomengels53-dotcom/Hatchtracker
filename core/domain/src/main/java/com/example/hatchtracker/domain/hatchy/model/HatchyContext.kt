package com.example.hatchtracker.domain.hatchy.model

import com.example.hatchtracker.data.models.SubscriptionTier

enum class HatchyModule {
    HOME,
    FLOCK,
    INCUBATION,
    NURSERY,
    FINANCE,
    BREEDING,
    SUPPORT,
    ADMIN,
    DASHBOARD
}

enum class UnitsSystem {
    METRIC,
    IMPERIAL
}

data class HatchyContext(
    val currentModule: HatchyModule = HatchyModule.HOME,
    val currentRoute: String = "main_menu/home",
    val screenTitle: String? = null,
    val selectedSpecies: String? = null,
    val selectedBreed: String? = null,
    val currentEntityId: String? = null,
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val isAdminOrDeveloper: Boolean = false,
    val localeTag: String = "en", // Default to language code only for bundle matching
    val units: UnitsSystem = UnitsSystem.METRIC,
    val timestamp: Long = System.currentTimeMillis()
)
