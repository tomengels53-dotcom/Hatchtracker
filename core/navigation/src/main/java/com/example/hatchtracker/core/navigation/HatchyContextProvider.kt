package com.example.hatchtracker.core.navigation

import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.model.HatchyModule
import com.example.hatchtracker.domain.hatchy.routing.IHatchyContextProvider
import com.example.hatchtracker.data.models.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that provides the current UI context for Hatchy.
 * Implements the domain interface to keep navigation logic decoupled.
 */
@Singleton
class HatchyContextProvider @Inject constructor() : IHatchyContextProvider {

    private val _context = MutableStateFlow(HatchyContext())
    override val context: StateFlow<HatchyContext> = _context.asStateFlow()

    fun updateContext(
        route: String,
        tier: SubscriptionTier,
        isAdmin: Boolean = false,
        species: String? = null,
        breed: String? = null,
        entityId: String? = null
    ) {
        val module = detectModule(route)
        _context.value = _context.value.copy(
            currentRoute = route,
            currentModule = module,
            tier = tier,
            isAdminOrDeveloper = isAdmin,
            selectedSpecies = species,
            selectedBreed = breed,
            currentEntityId = entityId,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun detectModule(route: String): HatchyModule {
        return when {
            route.contains("main_menu/home") -> HatchyModule.HOME
            route.contains("flock") || route.contains("bird") -> HatchyModule.FLOCK
            route.contains("incubation") || route.contains("hatch") -> HatchyModule.INCUBATION
            route.contains("nursery") -> HatchyModule.NURSERY
            route.contains("financial") || route.contains("sales") -> HatchyModule.FINANCE
            route.contains("breeding") -> HatchyModule.BREEDING
            route.contains("help_support") || route.contains("hatchy") -> HatchyModule.SUPPORT
            route.contains("admin") -> HatchyModule.ADMIN
            else -> HatchyModule.HOME
        }
    }
}
