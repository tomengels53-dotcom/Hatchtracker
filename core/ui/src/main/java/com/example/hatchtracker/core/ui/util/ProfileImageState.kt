package com.example.hatchtracker.core.ui.util

import androidx.annotation.DrawableRes

/**
 * Represents the resolved state of a profile image for display.
 */
sealed class ProfileImageState {
    /**
     * A valid local image file path.
     */
    data class LocalPhoto(val path: String) : ProfileImageState()

    /**
     * A species-specific placeholder icon.
     */
    data class SpeciesIcon(@param:DrawableRes val resId: Int) : ProfileImageState()

    /**
     * A generic fallback icon (e.g., gender-based).
     */
    data class GenericFallback(@param:DrawableRes val resId: Int) : ProfileImageState()
}
