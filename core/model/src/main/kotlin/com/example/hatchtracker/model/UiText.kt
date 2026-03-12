package com.example.hatchtracker.model

/**
 * A sealed class for handling UI text that can be used in pure Kotlin modules (domain/model).
 * Resolution typically happens in the UI layer.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    class StringResource(
        val resId: Int,
        vararg val args: Any
    ) : UiText()
}
