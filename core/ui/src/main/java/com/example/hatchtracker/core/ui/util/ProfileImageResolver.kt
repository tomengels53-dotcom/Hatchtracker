package com.example.hatchtracker.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.hatchtracker.core.ui.catalog.DefaultCatalogData
import com.example.hatchtracker.model.Species
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.example.hatchtracker.core.ui.R

/**
 * Resolves the appropriate ProfileImageState for a given entity.
 * Logic:
 * 1. imagePath (if file exists locally) -> LocalPhoto
 * 2. Species catalog mapping -> SpeciesIcon
 * 3. Generic fallback -> GenericFallback
 */
@Composable
fun rememberProfileImageState(
    imagePath: String?,
    species: Species,
    genderIcon: Int? = null
): ProfileImageState {
    var state by remember(imagePath, species, genderIcon) {
        mutableStateOf<ProfileImageState>(
            resolvePlaceholder(species, genderIcon)
        )
    }

    LaunchedEffect(imagePath, species) {
        if (imagePath != null) {
            val exists = withContext(Dispatchers.IO) {
                try {
                    File(imagePath).exists()
                } catch (e: Exception) {
                    false
                }
            }
            if (exists) {
                state = ProfileImageState.LocalPhoto(imagePath)
            } else {
                state = resolvePlaceholder(species, genderIcon)
            }
        } else {
            state = resolvePlaceholder(species, genderIcon)
        }
    }

    return state
}

private fun resolvePlaceholder(species: Species, genderIcon: Int?): ProfileImageState {
    val catalogEntry = DefaultCatalogData.allSpecies.find { 
        it.id.equals(species.name, ignoreCase = true) 
    }
    
    return when {
        catalogEntry?.imageResId != null -> ProfileImageState.SpeciesIcon(catalogEntry.imageResId)
        genderIcon != null -> ProfileImageState.GenericFallback(genderIcon)
        else -> ProfileImageState.GenericFallback(com.example.hatchtracker.core.ui.R.drawable.chick)
    }
}
